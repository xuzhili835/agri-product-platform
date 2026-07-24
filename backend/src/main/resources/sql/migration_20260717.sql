-- ============================================================
-- 增量迁移 2026-07-17：融资申请表增加「申请原因」「还款来源」字段
-- 说明：
--   * 与 agri_platform.sql 互相独立，只跑这一个文件即可升级到最新结构。
--   * 可重复执行（列已存在则跳过），不会删除/修改任何现有数据。
--   * 配合本次「农户融资申请」模块升级：后端实体/DTO、前端表单、撤销(物理删除)功能。
-- 背景：tb_finance 原本没有"申请原因/还款来源"字段，本次新增以便融资申请更专业。
-- 执行：USE agri_platform; SOURCE database/migration_20260717.sql;
-- ============================================================
USE agri_platform;

-- 1) 申请原因 / 融资用途 purpose
SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'agri_platform'
    AND TABLE_NAME = 'tb_finance'
    AND COLUMN_NAME = 'purpose'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE tb_finance ADD COLUMN purpose VARCHAR(500) DEFAULT NULL COMMENT ''申请原因/融资用途'' AFTER remark',
  'SELECT ''purpose 列已存在，跳过'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 还款来源 repayment_source
SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'agri_platform'
    AND TABLE_NAME = 'tb_finance'
    AND COLUMN_NAME = 'repayment_source'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE tb_finance ADD COLUMN repayment_source VARCHAR(500) DEFAULT NULL COMMENT ''还款来源'' AFTER purpose',
  'SELECT ''repayment_source 列已存在，跳过'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
