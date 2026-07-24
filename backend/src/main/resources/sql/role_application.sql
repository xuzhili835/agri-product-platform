-- 角色申请表（农户/买家申请成为 专家/银行，管理员审核）
-- 在 agri_platform 库中执行：
--   USE agri_platform;
--   SOURCE src/main/resources/sql/role_application.sql;

CREATE TABLE IF NOT EXISTS tb_role_application (
  id            INT          NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  user_name     VARCHAR(64)  NOT NULL COMMENT '申请人用户名',
  target_role   VARCHAR(20)  NOT NULL COMMENT '申请目标角色 expert/bank',
  real_name     VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  profession    VARCHAR(100) DEFAULT NULL COMMENT '专业(申请专家时填写)',
  position      VARCHAR(100) DEFAULT NULL COMMENT '职位(申请专家时填写)',
  belong        VARCHAR(100) DEFAULT NULL COMMENT '所属单位(专家)/银行名称(银行)',
  reason        VARCHAR(255) DEFAULT NULL COMMENT '申请理由',
  materials     VARCHAR(500) DEFAULT NULL COMMENT '相关材料文件URL(资质/证明,可选)',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2驳回',
  review_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  reviewer      VARCHAR(64)  DEFAULT NULL COMMENT '审核人',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_user_name (user_name),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色申请表';
