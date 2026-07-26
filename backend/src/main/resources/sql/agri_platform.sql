-- ========================================
-- 农产品融销一体平台 - 数据库建表脚本
-- 版本：v1.0
-- 创建时间：2026-07-07
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `agri_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `agri_platform`;

-- ========================================
-- 1. 用户表 (tb_user)
-- ========================================
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
  `user_name` varchar(64) NOT NULL COMMENT '用户名（主键）',
  `password` varchar(255) NOT NULL COMMENT '密码（加密存储）',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `identity_num` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `role` varchar(20) NOT NULL DEFAULT 'farmer' COMMENT '角色：farmer农户/buyer买家/expert专家/bank银行/admin管理员',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `integral` int DEFAULT 500 COMMENT '积分',
  `credit` int DEFAULT 5 COMMENT '信誉分（1-5）',
  `status` tinyint DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_name`),
  KEY `idx_role` (`role`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 2. 收货地址表 (tb_address)
-- ========================================
DROP TABLE IF EXISTS `tb_address`;
CREATE TABLE `tb_address` (
  `id` int NOT NULL AUTO_INCREMENT,
  `own_name` varchar(64) NOT NULL COMMENT '所属用户名',
  `consignee` varchar(64) NOT NULL COMMENT '收货人',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `address_detail` varchar(255) NOT NULL COMMENT '详细地址',
  `is_default` tinyint DEFAULT 0 COMMENT '是否默认地址：1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_own_name` (`own_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ========================================
-- 3. 商品表 (tb_product)
-- ========================================
DROP TABLE IF EXISTS `tb_product`;
CREATE TABLE `tb_product` (
  `order_id` int NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `content` varchar(500) NOT NULL COMMENT '商品描述',
  `picture` varchar(255) DEFAULT NULL COMMENT '商品图片URL',
  `type` varchar(20) NOT NULL COMMENT '类型：goods货源/demand需求',
  `order_status` tinyint DEFAULT 0 COMMENT '状态：0待交易 1交易中 2已完成',
  `own_name` varchar(64) NOT NULL COMMENT '发布者用户名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  KEY `idx_own_name` (`own_name`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ========================================
-- 4. 购物车表 (tb_shoppingcart)
-- ========================================
DROP TABLE IF EXISTS `tb_shoppingcart`;
CREATE TABLE `tb_shoppingcart` (
  `shopping_id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL COMMENT '商品ID',
  `count` int NOT NULL COMMENT '数量',
  `own_name` varchar(64) NOT NULL COMMENT '所属用户名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`shopping_id`),
  KEY `idx_own_name` (`own_name`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ========================================
-- 5. 订单表 (tb_purchase)
-- ========================================
DROP TABLE IF EXISTS `tb_purchase`;
CREATE TABLE `tb_purchase` (
  `purchase_id` int NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `own_name` varchar(64) NOT NULL COMMENT '下单用户名',
  `total_price` decimal(10,2) NOT NULL COMMENT '订单总价',
  `address` varchar(255) NOT NULL COMMENT '收货地址',
  `purchase_status` tinyint DEFAULT 1 COMMENT '订单状态：1待付款 2待发货 3待收货 4已完成 5已取消',
  `purchase_type` tinyint NOT NULL COMMENT '订单类型：1购物车 2直接购买',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`purchase_id`),
  KEY `idx_own_name` (`own_name`),
  KEY `idx_status` (`purchase_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========================================
-- 6. 订单详情表 (tb_purchase_detail)
-- ========================================
DROP TABLE IF EXISTS `tb_purchase_detail`;
CREATE TABLE `tb_purchase_detail` (
  `detail_id` int NOT NULL AUTO_INCREMENT,
  `purchase_id` int NOT NULL COMMENT '订单ID',
  `order_id` int NOT NULL COMMENT '商品ID',
  `unin_price` decimal(10,2) NOT NULL COMMENT '单价',
  `count` int NOT NULL COMMENT '数量',
  `sum_price` decimal(10,2) NOT NULL COMMENT '小计',
  PRIMARY KEY (`detail_id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表';

-- ========================================
-- 6.1 支付流水表 (tb_payment) —— 支付宝支付记录
-- ========================================
DROP TABLE IF EXISTS `tb_payment`;
CREATE TABLE `tb_payment` (
  `payment_id` int NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
  `purchase_id` int NOT NULL COMMENT '订单ID',
  `out_trade_no` varchar(64) NOT NULL COMMENT '商户订单号(=purchase_id)',
  `alipay_trade_no` varchar(64) DEFAULT NULL COMMENT '支付宝交易号',
  `total_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `trade_status` varchar(32) DEFAULT NULL COMMENT '交易状态',
  `pay_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_purchase_id` (`purchase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付宝支付流水表';

-- ========================================
-- 7. 融资产品表 (tb_finance_product)
-- ========================================
DROP TABLE IF EXISTS `tb_finance_product`;
CREATE TABLE `tb_finance_product` (
  `product_id` int NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `bank_name` varchar(100) NOT NULL COMMENT '银行名称',
  `bank_user_name` varchar(64) DEFAULT NULL COMMENT '所属银行账号(userName)，用于银行间数据隔离',
  `introduce` varchar(500) NOT NULL COMMENT '银行介绍',
  `bank_phone` varchar(20) NOT NULL COMMENT '联系电话',
  `money` decimal(12,2) NOT NULL COMMENT '可贷金额',
  `rate` decimal(5,2) NOT NULL COMMENT '年利率(%)',
  `repayment` int NOT NULL COMMENT '还款期限(月)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融资产品表';

-- ========================================
-- 8. 融资申请表 (tb_finance)
-- ========================================
DROP TABLE IF EXISTS `tb_finance`;
CREATE TABLE `tb_finance` (
  `finance_id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `product_id` int NOT NULL COMMENT '融资产品ID',
  `bank_user_name` varchar(64) DEFAULT NULL COMMENT '所属银行账号(userName)，用于银行间数据隔离',
  `own_name` varchar(64) NOT NULL COMMENT '申请人用户名',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `id_num` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `money` decimal(12,2) NOT NULL COMMENT '申请金额',
  `rate` decimal(5,2) DEFAULT NULL COMMENT '年利率',
  `repayment` int DEFAULT NULL COMMENT '还款期限(月)',
  `status` tinyint DEFAULT 0 COMMENT '状态：0申请中 1已通过 2已驳回',
  `remark` varchar(255) DEFAULT NULL COMMENT '审批备注',
  `purpose` varchar(500) DEFAULT NULL COMMENT '申请原因/融资用途',
  `repayment_source` varchar(500) DEFAULT NULL COMMENT '还款来源',
  `combination_name1` varchar(64) DEFAULT NULL COMMENT '联合人1姓名',
  `combination_phone1` varchar(20) DEFAULT NULL COMMENT '联合人1电话',
  `combination_idnum1` varchar(20) DEFAULT NULL COMMENT '联合人1身份证',
  `combination_name2` varchar(64) DEFAULT NULL COMMENT '联合人2姓名',
  `combination_phone2` varchar(20) DEFAULT NULL COMMENT '联合人2电话',
  `combination_idnum2` varchar(20) DEFAULT NULL COMMENT '联合人2身份证',
  `file_info` text COMMENT '附件信息(JSON格式)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`finance_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_own_name` (`own_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融资申请表';

-- ========================================
-- 9. 融资意向表 (tb_financing_intention)
-- ========================================
DROP TABLE IF EXISTS `tb_financing_intention`;
CREATE TABLE `tb_financing_intention` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_name` varchar(64) NOT NULL COMMENT '用户名',
  `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `address` varchar(255) NOT NULL COMMENT '地址',
  `amount` int NOT NULL COMMENT '期望金额(万)',
  `application` varchar(255) NOT NULL COMMENT '融资用途',
  `item` varchar(100) NOT NULL COMMENT '农作物',
  `area` int DEFAULT NULL COMMENT '种植面积(亩)',
  `repayment_period` int NOT NULL COMMENT '还款期限(月)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融资意向表';

-- ========================================
-- 10. 专家信息表 (tb_expert)
-- ========================================
DROP TABLE IF EXISTS `tb_expert`;
CREATE TABLE `tb_expert` (
  `user_name` varchar(64) NOT NULL COMMENT '用户名（主键）',
  `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `profession` varchar(100) NOT NULL COMMENT '专业领域',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `belong` varchar(100) DEFAULT NULL COMMENT '所属单位',
  PRIMARY KEY (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家信息表';

-- ========================================
-- 11. 农业知识表 (tb_knowledge)
-- ========================================
DROP TABLE IF EXISTS `tb_knowledge`;
CREATE TABLE `tb_knowledge` (
  `knowledge_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '标题',
  `content` text NOT NULL COMMENT '内容',
  `pic_path` varchar(255) DEFAULT NULL COMMENT '封面图片URL',
  `own_name` varchar(64) NOT NULL COMMENT '发布者用户名',
  `status` tinyint DEFAULT 1 COMMENT '状态：1发布 0草稿',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`knowledge_id`),
  KEY `idx_own_name` (`own_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农业知识表';

-- ========================================
-- 12. 知识评论表 (tb_discuss)
-- ========================================
DROP TABLE IF EXISTS `tb_discuss`;
CREATE TABLE `tb_discuss` (
  `discuss_id` int NOT NULL AUTO_INCREMENT,
  `knowledge_id` int NOT NULL COMMENT '知识ID',
  `own_name` varchar(64) NOT NULL COMMENT '评论人用户名',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`discuss_id`),
  KEY `idx_knowledge_id` (`knowledge_id`),
  KEY `idx_own_name` (`own_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识评论表';

-- ========================================
-- 13. 问答表 (tb_question)
-- ========================================
DROP TABLE IF EXISTS `tb_question`;
CREATE TABLE `tb_question` (
  `id` int NOT NULL AUTO_INCREMENT,
  `expert_name` varchar(64) NOT NULL COMMENT '被提问专家用户名',
  `expert_real_name` varchar(64) DEFAULT NULL COMMENT '专家真实姓名',
  `questioner` varchar(64) NOT NULL COMMENT '提问者用户名',
  `questioner_real_name` varchar(64) DEFAULT NULL COMMENT '提问者真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `plant_name` varchar(64) NOT NULL COMMENT '农作物名称',
  `title` varchar(255) NOT NULL COMMENT '问题标题',
  `question` text COMMENT '问题详细内容',
  `answer` text COMMENT '专家回答',
  `status` tinyint DEFAULT 0 COMMENT '状态：0未回答 1已回答 2已关闭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_expert_name` (`expert_name`),
  KEY `idx_questioner` (`questioner`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答表';

-- ========================================
-- 14. 专家预约表 (tb_reserve)
-- ========================================
DROP TABLE IF EXISTS `tb_reserve`;
CREATE TABLE `tb_reserve` (
  `id` int NOT NULL AUTO_INCREMENT,
  `expert_name` varchar(64) NOT NULL COMMENT '专家用户名',
  `expert_real_name` varchar(64) DEFAULT NULL COMMENT '专家真实姓名',
  `questioner` varchar(64) NOT NULL COMMENT '预约人用户名',
  `questioner_real_name` varchar(64) DEFAULT NULL COMMENT '预约人真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `address` varchar(255) NOT NULL COMMENT '地址',
  `area` varchar(64) NOT NULL COMMENT '面积',
  `plant_name` varchar(64) NOT NULL COMMENT '农作物名称',
  `soil_condition` varchar(255) NOT NULL COMMENT '土壤条件',
  `plant_condition` varchar(255) NOT NULL COMMENT '作物条件',
  `plant_detail` varchar(500) NOT NULL COMMENT '作物详情',
  `message` varchar(255) DEFAULT NULL COMMENT '留言',
  `preferred_time` varchar(64) DEFAULT NULL COMMENT '期望时间段',
  `answer` varchar(255) DEFAULT NULL COMMENT '专家回复',
  `status` tinyint DEFAULT 0 COMMENT '状态：0待处理 1已完成 2已拒绝',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_expert_name` (`expert_name`),
  KEY `idx_questioner` (`questioner`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家预约表';

-- ========================================
-- 14.1 问题追问回复表 (tb_question_reply)
-- ========================================
DROP TABLE IF EXISTS `tb_question_reply`;
CREATE TABLE `tb_question_reply` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int NOT NULL COMMENT '所属问题ID（tb_question.id）',
  `author_user_name` varchar(64) NOT NULL COMMENT '回复人用户名',
  `author_real_name` varchar(64) DEFAULT NULL COMMENT '回复人真实姓名（冗余）',
  `author_role` varchar(16) NOT NULL COMMENT '回复人角色：farmer追问 / expert回答',
  `content` text NOT NULL COMMENT '回复内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_author` (`author_user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题追问回复表';

-- ========================================
-- 15. 轮播图表 (tb_banner)
-- ========================================
DROP TABLE IF EXISTS `tb_banner`;
CREATE TABLE `tb_banner` (
  `banner_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL COMMENT '标题',
  `pic_path` varchar(255) NOT NULL COMMENT '图片URL',
  `link_url` varchar(255) DEFAULT NULL COMMENT '跳转链接',
  `sort_order` int DEFAULT 0 COMMENT '排序值',
  `status` tinyint DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`banner_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- ========================================
-- 初始化测试数据
-- ========================================

-- 插入管理员账号（密码：123456）
INSERT INTO `tb_user` (`user_name`, `password`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '系统管理员', 'admin', 1);

-- 插入测试农户账号（密码：123456）
INSERT INTO `tb_user` (`user_name`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('farmer01', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '李建国', '13800138001', 'farmer', 1),
('farmer02', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '王桂芳', '13800138002', 'farmer', 1);

-- 插入测试买家账号（密码：123456）
INSERT INTO `tb_user` (`user_name`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('buyer01', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '赵晓东', '13800138005', 'buyer', 1);

-- 插入测试银行账号（密码：123456）
INSERT INTO `tb_user` (`user_name`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('bank01', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '青岛银行', '0532-96588', 'bank', 1);

-- 插入测试专家账号（密码：123456）
INSERT INTO `tb_user` (`user_name`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('expert01', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '王教授', '13800138003', 'expert', 1),
('expert02', '$2a$10$nJnOhRdK10BXg76ET3vafugVrU7vQdNAgWQmd2vyTehVlcaemty7', '刘博士', '13800138004', 'expert', 1);

-- 插入专家详细信息
INSERT INTO `tb_expert` (`user_name`, `real_name`, `phone`, `profession`, `position`, `belong`) VALUES
('expert01', '王教授', '13800138003', '农学', '教授', '山东省农科院'),
('expert02', '刘博士', '13800138004', '作物栽培', '研究员', '青岛农业大学');

-- 插入测试融资产品
INSERT INTO `tb_finance_product` (`bank_name`, `introduce`, `bank_phone`, `money`, `rate`, `repayment`) VALUES
('青岛银行', '青易贷，助力小微企业和农户成长发展', '96588', 100000, 3.5, 24),
('中国农业银行', '惠农贷，专为农户设计的低息贷款产品', '95599', 150000, 3.2, 36),
('中国邮政储蓄银行', '小额贷，手续简便，审批快速', '95580', 80000, 3.8, 12);

-- 插入测试轮播图
INSERT INTO `tb_banner` (`title`, `pic_path`, `link_url`, `sort_order`, `status`) VALUES
('欢迎来到农产品融销平台', '/images/banner1.jpg', '/home', 1, 1),
('优质农产品交易', '/images/banner2.jpg', '/product', 2, 1),
('专业农业咨询服务', '/images/banner3.jpg', '/expert', 3, 1);

-- 完成

-- ============================================================
-- 角色申请表（农户/买家申请成为 专家/银行，管理员审核）
-- ============================================================
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
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2驳回',
  review_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  reviewer      VARCHAR(64)  DEFAULT NULL COMMENT '审核人',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_user_name (user_name),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色申请表';
