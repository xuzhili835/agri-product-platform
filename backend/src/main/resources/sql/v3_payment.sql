-- ============================================================
-- v3_payment.sql  支付宝支付模块 · 增量迁移（只跑这一个文件，不重跑 agri_platform.sql）
--   * 新增 tb_payment 支付流水表：记录支付宝交易号 / 金额 / 交易状态 / 支付时间
--   * 与既有数据互相独立，老库执行后即可使用支付功能，不会丢已有订单数据。
-- 执行：USE agri_platform; SOURCE src/main/resources/sql/v3_payment.sql;
-- ============================================================

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
