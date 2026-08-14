package com.agri.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 启动时自动执行的数据库迁移（幂等）。
 * <p>
 * 每次后端启动（含在 IDEA 中运行）都会执行一次：先用 information_schema 检查目标列是否已存在，
 * 不存在才执行 ALTER TABLE ADD COLUMN，存在则跳过 —— 因此可重复执行、不会破坏现有数据。
 * <p>
 * 目的：团队成员只需在 IDEA 里启动后端，数据库就会自动升级到最新结构，无需手动跑 SQL 脚本。
 * 对应的人工脚本仍保留在 backend/database/ 下，供命令行手动执行（结果与本 Runner 一致）。
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DB迁移] 开始执行幂等数据库迁移（若目标列已存在则自动跳过）...");

        // 2026-07-14：角色申请表增加「相关材料」字段（对应 database/migration_20260714.sql）
        addColumnIfNotExists("tb_role_application", "materials",
                "VARCHAR(500) DEFAULT NULL COMMENT '相关材料文件URL（资质/证明等，可选）'");

        // 2026-07-17：融资申请表增加「申请原因 / 还款来源」（对应 database/migration_20260717.sql）
        addColumnIfNotExists("tb_finance", "purpose",
                "VARCHAR(500) DEFAULT NULL COMMENT '申请原因/融资用途'");
        addColumnIfNotExists("tb_finance", "repayment_source",
                "VARCHAR(500) DEFAULT NULL COMMENT '还款来源'");

        // 2026-07-20：地址表增加省/市/区（原 migration_add_address_region_7.20.sql 是非幂等裸 ALTER，
        // 收口到此处幂等执行；所有机器重启后端即自动具备三级地址列）
        addColumnIfNotExists("tb_address", "province",
                "VARCHAR(50) DEFAULT NULL COMMENT '省'");
        addColumnIfNotExists("tb_address", "city",
                "VARCHAR(50) DEFAULT NULL COMMENT '市'");
        addColumnIfNotExists("tb_address", "area",
                "VARCHAR(50) DEFAULT NULL COMMENT '区'");

        // 2026-07-21：站内消息表（铃铛通知）。幂等建表，团队成员重启后端即自动具备。
        createTableIfNotExists("tb_message",
                "CREATE TABLE IF NOT EXISTS tb_message ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',"
                        + "user_name VARCHAR(50) NOT NULL COMMENT '接收人 userName',"
                        + "category VARCHAR(32) DEFAULT NULL COMMENT '分类: order/reserve/finance/question/system',"
                        + "title VARCHAR(200) NOT NULL COMMENT '标题',"
                        + "content VARCHAR(500) DEFAULT NULL COMMENT '正文',"
                        + "link_url VARCHAR(255) DEFAULT NULL COMMENT '点击跳转路径(可选)',"
                        + "is_read TINYINT DEFAULT 0 COMMENT '0未读 1已读',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                        + "INDEX idx_message_user_read (user_name, is_read)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '站内消息'");

        // 2026-07-21：联合贷款人邀请表。农户选联系人发起邀请 → 对方同意后回填其资料到 tb_finance。
        // 幂等建表，团队成员重启后端即自动具备。
        createTableIfNotExists("tb_joint_invitation",
                "CREATE TABLE IF NOT EXISTS tb_joint_invitation ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',"
                        + "finance_id INT NOT NULL COMMENT '融资申请ID(tb_finance.finance_id)',"
                        + "applicant_user_name VARCHAR(50) NOT NULL COMMENT '申请人(农户) userName',"
                        + "joint_user_name VARCHAR(50) NOT NULL COMMENT '被邀请的联合贷款人 userName',"
                        + "slot TINYINT NOT NULL DEFAULT 1 COMMENT '联合人位置 1或2',"
                        + "status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已同意 2已拒绝',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                        + "handle_time DATETIME DEFAULT NULL COMMENT '处理时间',"
                        + "INDEX idx_joint_invitation_finance (finance_id),"
                        + "INDEX idx_joint_invitation_joint (joint_user_name)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '联合贷款人邀请'");

        // 2026-07-21：还款计划表。融资审批通过后按等额本息自动生成各期还款计划。
        createTableIfNotExists("tb_repayment",
                "CREATE TABLE IF NOT EXISTS tb_repayment ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',"
                        + "finance_id INT NOT NULL COMMENT '融资申请ID(tb_finance.finance_id)',"
                        + "period_index INT NOT NULL COMMENT '期数 1..N',"
                        + "due_date DATE DEFAULT NULL COMMENT '到期日',"
                        + "principal DECIMAL(14,2) DEFAULT 0 COMMENT '本期本金',"
                        + "interest DECIMAL(14,2) DEFAULT 0 COMMENT '本期利息',"
                        + "total_amount DECIMAL(14,2) DEFAULT 0 COMMENT '本期应还本息',"
                        + "paid_amount DECIMAL(14,2) DEFAULT 0 COMMENT '已还金额',"
                        + "status TINYINT NOT NULL DEFAULT 0 COMMENT '0未还 1已还 2待确认(农户已提交) 3已驳回（逾期读取时动态判定）',"
                        + "paid_time DATETIME DEFAULT NULL COMMENT '实还/提交时间',"
                        + "transaction_no VARCHAR(64) DEFAULT NULL COMMENT '还款流水号/备注（农户提交时填写）',"
                        + "pay_proof VARCHAR(512) DEFAULT NULL COMMENT '还款凭证图片URL（农户提交，经 /upload 上传）',"
                        + "reject_reason VARCHAR(255) DEFAULT NULL COMMENT '银行驳回原因',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                        + "INDEX idx_repayment_finance (finance_id),"
                        + "INDEX idx_repayment_status (status)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '还款计划'");

        // 2026-07-21：还款计划表增加「流水号 / 凭证 / 驳回原因」三列，支撑还贷两阶段(农户提交→银行确认)。
        // 既有库（表已存在但缺这三列）重启后端即自动补齐；全新库则上面的 CREATE 已含。
        addColumnIfNotExists("tb_repayment", "transaction_no",
                "VARCHAR(64) DEFAULT NULL COMMENT '还款流水号/备注（农户提交时填写）'");
        addColumnIfNotExists("tb_repayment", "pay_proof",
                "VARCHAR(512) DEFAULT NULL COMMENT '还款凭证图片URL（农户提交，经 /upload 上传）'");
        addColumnIfNotExists("tb_repayment", "reject_reason",
                "VARCHAR(255) DEFAULT NULL COMMENT '银行驳回原因'");

        // 2026-07-21：融资产品表增加「上下架状态」。融资产品不可删除，只能暂停供应：
        // 暂停(status=1)后农户无法再申请，但已申请记录仍保留在审批中。默认 0=在售。
        addColumnIfNotExists("tb_finance_product", "status",
                "TINYINT NOT NULL DEFAULT 0 COMMENT '0在售/可申请 1暂停供应'");

        // 2026-07-22：融资产品增加「产品名称」。银行可发布多个产品，产品名与银行名分离：
        // 银行名固定取自该银行注册信息(realName)，产品名由银行自行命名（如"助农贷"）。
        addColumnIfNotExists("tb_finance_product", "product_name",
                "VARCHAR(100) DEFAULT NULL COMMENT '产品名称（由银行命名，如：助农贷）'");

        // 2026-07-22：银行间数据隔离。融资产品/融资申请增加「所属银行账号(bank_user_name)」，
        // 使每家银行只能看到/管理本行产品与对应申请。幂等加列 + 存量数据按 bank_name↔real_name 回填
        // （等价于手工脚本 v3_bank_isolation.sql，团队成员重启后端即自动具备，无需手动跑 v3）。
        addColumnIfNotExists("tb_finance_product", "bank_user_name",
                "VARCHAR(64) DEFAULT NULL COMMENT '发布该产品的银行账号(userName)，用于银行间数据隔离'");
        addColumnIfNotExists("tb_finance", "bank_user_name",
                "VARCHAR(64) DEFAULT NULL COMMENT '该申请所对应产品的银行账号(userName)，用于银行间数据隔离'");
        backfillBankUserName();

        // 2026-08-12：智能体(Agent)。会话/聊天历史/工具审计/全局开关/知识切块向量，全部幂等建表。
        createTableIfNotExists("tb_agent_session",
                "CREATE TABLE IF NOT EXISTS tb_agent_session ("
                        + "session_id VARCHAR(64) NOT NULL COMMENT '会话ID(UUID)' PRIMARY KEY,"
                        + "user_name VARCHAR(50) NOT NULL COMMENT '发起人 userName',"
                        + "role VARCHAR(16) NOT NULL COMMENT '角色 farmer/buyer',"
                        + "status TINYINT NOT NULL DEFAULT 1 COMMENT '1进行中 0已关闭',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_agent_session_user (user_name)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'agent会话'");

        createTableIfNotExists("tb_agent_message",
                "CREATE TABLE IF NOT EXISTS tb_agent_message ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "session_id VARCHAR(64) NOT NULL COMMENT '会话ID',"
                        + "user_name VARCHAR(50) NOT NULL,"
                        + "direction VARCHAR(16) NOT NULL COMMENT 'user/assistant',"
                        + "content TEXT COMMENT '消息文本',"
                        + "tool_event VARCHAR(500) DEFAULT NULL COMMENT '工具事件摘要JSON(如调用了query_credit)',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_agent_message_session (session_id, create_time)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'agent聊天历史(档2可见消息)'");

        createTableIfNotExists("tb_agent_tool_log",
                "CREATE TABLE IF NOT EXISTS tb_agent_tool_log ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "session_id VARCHAR(64) NOT NULL,"
                        + "user_name VARCHAR(50) NOT NULL,"
                        + "tool_name VARCHAR(64) NOT NULL,"
                        + "arguments VARCHAR(2000) DEFAULT NULL COMMENT '入参JSON(脱敏后)',"
                        + "result VARCHAR(2000) DEFAULT NULL COMMENT '结果JSON(脱敏后)',"
                        + "status VARCHAR(16) NOT NULL COMMENT 'ok/error/pending/cancelled/timeout',"
                        + "duration_ms INT DEFAULT NULL,"
                        + "confirmed TINYINT DEFAULT 0 COMMENT '写操作是否经用户确认 0否1是',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_agent_tool_log_session (session_id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'agent工具审计'");

        createTableIfNotExists("tb_system_config",
                "CREATE TABLE IF NOT EXISTS tb_system_config ("
                        + "config_key VARCHAR(64) NOT NULL COMMENT '键' PRIMARY KEY,"
                        + "config_value VARCHAR(500) DEFAULT NULL COMMENT '值',"
                        + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统配置(如agent总开关)'");
        // agent 总开关默认开启(幂等：仅在不存在时插入)
        try {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_system_config WHERE config_key = ?",
                    Integer.class, "agent_enabled");
            if (c != null && c == 0) {
                jdbcTemplate.update(
                        "INSERT INTO tb_system_config(config_key, config_value) VALUES(?, ?)",
                        "agent_enabled", "true");
            }
        } catch (Exception e) {
            log.warn("[DB迁移] agent开关默认值插入失败，已跳过：{}", e.getMessage());
        }

        createTableIfNotExists("tb_knowledge_chunk",
                "CREATE TABLE IF NOT EXISTS tb_knowledge_chunk ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "knowledge_id INT NOT NULL COMMENT '来源文章 tb_knowledge.knowledge_id',"
                        + "chunk_index INT NOT NULL COMMENT '块序号',"
                        + "content TEXT NOT NULL COMMENT '切块文本',"
                        + "embedding LONGBLOB COMMENT 'bge-m3 向量(1024维float序列化)',"
                        + "role_scope VARCHAR(16) DEFAULT 'common' COMMENT '角色范围 farmer/buyer/common',"
                        + "model VARCHAR(64) DEFAULT NULL,"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_knowledge_chunk_kid (knowledge_id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '知识切块+向量'");

        // 2026-08-12：agent 演示用知识种子(融资流程/补贴政策/操作指南)。幂等：按 title 不存在才插。
        seedKnowledgeIfNotExists("融资申请流程指南",
                "农户可在【融资大厅】查看各银行发布的融资套餐(含额度上限、利率、期限)。"
                + "选择合适套餐后提交融资申请，填写融资金额、期限、申请原因、还款来源。"
                + "如需提高通过率，可选择联合贷款人共同申请。提交后等待银行审批。"
                + "注：信用分影响审批通过率，不影响额度上限；额度由银行套餐决定。",
                "farmer");
        seedKnowledgeIfNotExists("农业补贴政策摘要",
                "种粮直补、农机购置补贴、设施农业补贴等可通过当地农业农村局申请。"
                + "大棚种植一般可申请设施农业补贴。补贴不影响融资额度，但稳定的补贴收入"
                + "有助于提升还款能力评估，间接提高融资匹配分。",
                "farmer");
        seedKnowledgeIfNotExists("平台买卖操作指南",
                "农户在【我要卖货】发布商品(标明品类、价格、库存、图片)；买家在【市场】搜索下单，"
                + "下单后联系农户确认发货与收货。交易全程留痕，可在【我的订单】查看。",
                "common");

        log.info("[DB迁移] 完成。");
    }

    /**
     * 幂等加列：列已存在则跳过，不存在则 ADD COLUMN。
     * 使用 DATABASE() 取当前连接的库（即 application.yml 中 url 指向的 agri_platform），避免硬编码库名。
     */
    private void addColumnIfNotExists(String table, String column, String columnDefinition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);

            if (count != null && count > 0) {
                log.info("[DB迁移] {}.{} 列已存在，跳过", table, column);
                return;
            }

            String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + columnDefinition;
            jdbcTemplate.execute(sql);
            log.info("[DB迁移] 已新增列 {}.{}", table, column);
        } catch (Exception e) {
            // 表尚未建好（新环境未导入建表脚本）等情况，打印警告但不阻断启动
            log.warn("[DB迁移] 新增列 {}.{} 失败，已跳过（不阻断启动）：{}", table, column, e.getMessage());
        }
    }

    /**
     * 幂等建表：表已存在则跳过，不存在则 CREATE TABLE IF NOT EXISTS。
     * 用于消息表、还款表、信用日志表等新业务表——重启后端自动创建，团队成员无需手动跑 SQL。
     */
    private void createTableIfNotExists(String tableName, String createSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    Integer.class, tableName);

            if (count != null && count > 0) {
                log.info("[DB迁移] 表 {} 已存在，跳过", tableName);
                return;
            }

            jdbcTemplate.execute(createSql);
            log.info("[DB迁移] 已新建表 {}", tableName);
        } catch (Exception e) {
            // 基础表未建好等情况，打印警告但不阻断启动
            log.warn("[DB迁移] 建表 {} 失败，已跳过（不阻断启动）：{}", tableName, e.getMessage());
        }
    }

    /**
     * 银行间数据隔离的存量回填（幂等）：把历史遗留、bank_user_name 为空的记录按
     * bank_name ↔ role=bank 用户 real_name 尽力回填；已回填的(WHERE IS NULL)启动时自动跳过。
     * 等价于手工脚本 v3_bank_isolation.sql 的回填段，确保团队成员重启后端即与 v3 结果一致。
     */
    private void backfillBankUserName() {
        try {
            // 产品：按 bank_name 匹配 role=bank 用户的 real_name
            int n1 = jdbcTemplate.update(
                    "UPDATE tb_finance_product p JOIN tb_user u "
                            + "ON u.role = 'bank' AND u.real_name = p.bank_name "
                            + "SET p.bank_user_name = u.user_name "
                            + "WHERE p.bank_user_name IS NULL");
            // 申请：取其所申请产品的 bank_user_name
            int n2 = jdbcTemplate.update(
                    "UPDATE tb_finance f JOIN tb_finance_product p ON p.product_id = f.product_id "
                            + "SET f.bank_user_name = p.bank_user_name "
                            + "WHERE f.bank_user_name IS NULL");
            if (n1 > 0 || n2 > 0) {
                log.info("[DB迁移] 银行隔离回填：产品 {} 条、申请 {} 条", n1, n2);
            }
        } catch (Exception e) {
            // 列未就绪或基础表缺失等情况，打印警告但不阻断启动
            log.warn("[DB迁移] 银行隔离回填失败，已跳过（不阻断启动）：{}", e.getMessage());
        }
    }

    /** 幂等插入一篇 agent 知识种子文章：按 title 不存在才插入 tb_knowledge(status=1 发布)。 */
    private void seedKnowledgeIfNotExists(String title, String content, String roleScope) {
        try {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_knowledge WHERE title = ?", Integer.class, title);
            if (c != null && c == 0) {
                jdbcTemplate.update(
                        "INSERT INTO tb_knowledge(title, content, own_name, status, create_time, update_time) "
                                + "VALUES(?, ?, ?, 1, NOW(), NOW())",
                        title, content, "system");
                log.info("[DB迁移] 种子知识已插入：{}", title);
            }
        } catch (Exception e) {
            log.warn("[DB迁移] 种子知识插入失败({})，已跳过：{}", title, e.getMessage());
        }
    }
}
