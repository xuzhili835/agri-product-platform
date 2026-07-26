-- ============================================================
-- refresh_realnames_20260726.sql
-- 用途：把三个测试账号的 low 名字（张三/李四/王五）换成真实感名字，
--       并刷新所有"按 userName 关联、冗余存储了 real_name"的快照列。
-- 背景：realName 已锁定不可改（AdminController/UserController 不再 setRealName，
--       前端两个真实姓名输入框 disabled）。本脚本一次性把存量数据对齐到新名。
-- 幂等：可重复执行；对未被改名的用户是 no-op（JOIN 后值不变）。
-- 依赖：必须在 tb_user 改名之后再刷新快照列（本脚本顺序已保证）。
-- 执行：mysql -u root -p agri_platform < refresh_realnames_20260726.sql
-- ============================================================

-- ---------- 1) 改用户表（按 userName 主键，最稳） ----------
UPDATE tb_user SET real_name='李建国' WHERE user_name='farmer01';
UPDATE tb_user SET real_name='王桂芳' WHERE user_name='farmer02';
UPDATE tb_user SET real_name='赵晓东' WHERE user_name='buyer01';

-- ---------- 2) tb_product.own_name 存的是发布者 realName 快照（bug 源头） ----------
-- 三人旧名（张三/李四/王五）在表里唯一，按旧值精确刷新
UPDATE tb_product SET own_name='李建国' WHERE own_name='张三';
UPDATE tb_product SET own_name='王桂芳' WHERE own_name='李四';
UPDATE tb_product SET own_name='赵晓东' WHERE own_name='王五';

-- ---------- 3) 其余"按 userName 关联、冗余了 real_name"的快照列，JOIN 刷新成最新 ----------
-- tb_finance.real_name ← own_name(申请人userName)
UPDATE tb_finance f
  JOIN tb_user u ON f.own_name = u.user_name
  SET f.real_name = u.real_name;

-- tb_financing_intention.real_name ← user_name
UPDATE tb_financing_intention fi
  JOIN tb_user u ON fi.user_name = u.user_name
  SET fi.real_name = u.real_name;

-- tb_expert.real_name ← user_name（被改名的三人非专家，此处对专家行做一致性刷新）
UPDATE tb_expert e
  JOIN tb_user u ON e.user_name = u.user_name
  SET e.real_name = u.real_name;

-- tb_question：提问者 + 专家两个快照列
UPDATE tb_question q
  JOIN tb_user u ON q.questioner = u.user_name
  SET q.questioner_real_name = u.real_name;
UPDATE tb_question q
  JOIN tb_user u ON q.expert_name = u.user_name
  SET q.expert_real_name = u.real_name;

-- tb_reserve：预约人 + 专家两个快照列
UPDATE tb_reserve r
  JOIN tb_user u ON r.questioner = u.user_name
  SET r.questioner_real_name = u.real_name;
UPDATE tb_reserve r
  JOIN tb_user u ON r.expert_name = u.user_name
  SET r.expert_real_name = u.real_name;

-- tb_question_reply.author_real_name ← author_user_name
UPDATE tb_question_reply rep
  JOIN tb_user u ON rep.author_user_name = u.user_name
  SET rep.author_real_name = u.real_name;

-- tb_role_application.real_name ← user_name
UPDATE tb_role_application ra
  JOIN tb_user u ON ra.user_name = u.user_name
  SET ra.real_name = u.real_name;

-- ---------- 校验（可选，执行后人工看一眼行数对不对） ----------
-- SELECT user_name, real_name FROM tb_user WHERE user_name IN ('farmer01','farmer02','buyer01');
-- SELECT own_name, COUNT(*) FROM tb_product GROUP BY own_name;
