-- ======================================================================
-- v3_bank_isolation.sql  ——  银行间数据隔离（增量迁移）
-- ----------------------------------------------------------------------
-- 为「融资产品」与「融资申请」增加所属银行账号字段(bank_user_name)，
-- 使每家银行只能看到/管理本行产品及其对应的融资申请，禁止跨行查看与审批。
--
-- 本脚本为增量迁移：只新增列并尽力回填，不删除、不重建任何表，不影响既有业务数据。
-- 请在现有数据库上执行一次即可（与全量 agri_platform.sql 无关，不会造成数据丢失）。
-- ======================================================================

-- 1) 融资产品增加「所属银行账号」
ALTER TABLE tb_finance_product
    ADD COLUMN bank_user_name VARCHAR(64) NULL COMMENT '发布该产品的银行账号(userName)，用于银行间数据隔离';

-- 2) 融资申请增加「所属银行账号」（申请提交时由所申请产品的 bank_user_name 冗余写入）
ALTER TABLE tb_finance
    ADD COLUMN bank_user_name VARCHAR(64) NULL COMMENT '该申请所对应产品的银行账号(userName)，用于银行间数据隔离';

-- 3) 回填存量产品的所属银行：按 bank_name 匹配 role=bank 用户的 real_name（尽力回填，匹配不上的保持 NULL）
--    说明：bank_name 为发布时填写的银行/产品名称，通常等于该银行账号的 real_name。
UPDATE tb_finance_product p
JOIN tb_user u ON u.role = 'bank' AND u.real_name = p.bank_name
SET p.bank_user_name = u.user_name
WHERE p.bank_user_name IS NULL;

-- 4) 回填存量融资申请的所属银行：取其所申请产品的 bank_user_name
UPDATE tb_finance f
JOIN tb_finance_product p ON p.product_id = f.product_id
SET f.bank_user_name = p.bank_user_name
WHERE f.bank_user_name IS NULL;
