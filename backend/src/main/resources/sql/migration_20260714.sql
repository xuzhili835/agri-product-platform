-- ============================================================
-- 增量迁移 2026-07-14：角色申请表增加「相关材料」字段
-- 说明：
--   * 与 agri_platform.sql 互相独立，只跑这一个文件即可。
--   * 可重复执行（列已存在则跳过），不会删除/修改任何现有数据。
-- 执行：USE agri_platform; SOURCE src/main/resources/sql/migration_20260714.sql;
-- ============================================================
USE agri_platform;

-- 判断 materials 列是否已存在，不存在才 ADD（实现幂等）
SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'agri_platform'
    AND TABLE_NAME = 'tb_role_application'
    AND COLUMN_NAME = 'materials'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE tb_role_application ADD COLUMN materials VARCHAR(500) DEFAULT NULL COMMENT ''相关材料文件URL（资质/证明等，可选）''',
  'SELECT ''materials 列已存在，跳过'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
