-- ================================================================
-- 专家模块 V2 迁移：预约期望时间 / realName 冗余 / 追问回复表
-- 文件: v2_expert_overhaul.sql
-- 说明: 幂等迁移，可重复执行（列已存在则跳过，表已存在则跳过）。
--       增量修改，不影响已有数据。请在 MySQL 中对当前业务库执行：
--         source v2_expert_overhaul.sql
--       无需重新执行 agri_platform.sql。
-- ================================================================

SET @db := DATABASE();

-- ----------------------------------------------------------------
-- 1. tb_reserve 增加：期望时间段、预约人真实姓名、专家真实姓名
-- ----------------------------------------------------------------
-- 1.1 preferred_time（期望时间段）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tb_reserve' AND COLUMN_NAME = 'preferred_time');
SET @sql := IF(@col = 0,
               'ALTER TABLE tb_reserve ADD COLUMN preferred_time varchar(64) DEFAULT NULL COMMENT ''期望时间段'' AFTER message',
               'SELECT ''tb_reserve.preferred_time 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 questioner_real_name
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tb_reserve' AND COLUMN_NAME = 'questioner_real_name');
SET @sql := IF(@col = 0,
               'ALTER TABLE tb_reserve ADD COLUMN questioner_real_name varchar(64) DEFAULT NULL COMMENT ''预约人真实姓名'' AFTER questioner',
               'SELECT ''tb_reserve.questioner_real_name 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 expert_real_name
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tb_reserve' AND COLUMN_NAME = 'expert_real_name');
SET @sql := IF(@col = 0,
               'ALTER TABLE tb_reserve ADD COLUMN expert_real_name varchar(64) DEFAULT NULL COMMENT ''专家真实姓名'' AFTER expert_name',
               'SELECT ''tb_reserve.expert_real_name 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------
-- 2. tb_question 增加：提问者真实姓名、专家真实姓名
-- ----------------------------------------------------------------
-- 2.1 questioner_real_name
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tb_question' AND COLUMN_NAME = 'questioner_real_name');
SET @sql := IF(@col = 0,
               'ALTER TABLE tb_question ADD COLUMN questioner_real_name varchar(64) DEFAULT NULL COMMENT ''提问者真实姓名'' AFTER questioner',
               'SELECT ''tb_question.questioner_real_name 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 expert_real_name
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tb_question' AND COLUMN_NAME = 'expert_real_name');
SET @sql := IF(@col = 0,
               'ALTER TABLE tb_question ADD COLUMN expert_real_name varchar(64) DEFAULT NULL COMMENT ''专家真实姓名'' AFTER expert_name',
               'SELECT ''tb_question.expert_real_name 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------
-- 3. 追问回复表 tb_question_reply（多轮对话）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_question_reply` (
                                                   `id` int NOT NULL AUTO_INCREMENT,
                                                   `question_id` int NOT NULL COMMENT '所属问题ID（tb_question.id）',
                                                   `author_user_name` varchar(64) NOT NULL COMMENT '回复人用户名',
                                                   `author_real_name` varchar(64) DEFAULT NULL COMMENT '回复人真实姓名（冗余）',
                                                   `author_role` varchar(16) NOT NULL COMMENT '回复人角色：farmer追问 / expert回答',
                                                   `content` text NOT NULL COMMENT '回复内容',
                                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
                                                   PRIMARY KEY (`id`),
                                                   KEY `idx_question_id` (`question_id`),
                                                   KEY `idx_author` (`author_user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题追问回复表';

-- ----------------------------------------------------------------
-- 4. 回填老数据的 real_name（已有 questioner / expert_name 关联 tb_user）
-- ----------------------------------------------------------------
UPDATE `tb_question` q
    INNER JOIN `tb_user` u ON u.user_name = q.questioner
SET q.questioner_real_name = u.real_name
WHERE q.questioner_real_name IS NULL;

UPDATE `tb_question` q
    INNER JOIN `tb_user` u ON u.user_name = q.expert_name
SET q.expert_real_name = u.real_name
WHERE q.expert_real_name IS NULL;

UPDATE `tb_reserve` r
    INNER JOIN `tb_user` u ON u.user_name = r.questioner
SET r.questioner_real_name = u.real_name
WHERE r.questioner_real_name IS NULL;

UPDATE `tb_reserve` r
    INNER JOIN `tb_user` u ON u.user_name = r.expert_name
SET r.expert_real_name = u.real_name
WHERE r.expert_real_name IS NULL;

-- ----------------------------------------------------------------
-- 5. 历史已回答问题回填为首条专家回复（使新的对话视图不空白）
--    幂等：仅在该问题尚无任何回复时插入
-- ----------------------------------------------------------------
INSERT INTO `tb_question_reply` (`question_id`, `author_user_name`, `author_real_name`, `author_role`, `content`, `create_time`)
SELECT q.`id`, q.`expert_name`, q.`expert_real_name`, 'expert', q.`answer`, COALESCE(q.`update_time`, q.`create_time`)
FROM `tb_question` q
WHERE q.`answer` IS NOT NULL AND q.`answer` <> ''
  AND NOT EXISTS (SELECT 1 FROM `tb_ques

tion_reply` rep WHERE rep.`question_id` = q.`id`);