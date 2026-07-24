-- 为 tb_address 表添加省市区字段
ALTER TABLE tb_address
    ADD COLUMN province VARCHAR(50) COMMENT '省份' AFTER phone,
    ADD COLUMN city VARCHAR(50) COMMENT '城市' AFTER province,
    ADD COLUMN area VARCHAR(50) COMMENT '区县' AFTER city;

-- 更新现有数据的 addressDetail（如果需要）
-- UPDATE tb_address SET addressDetail = CONCAT(IFNULL(province, ''), IFNULL(city, ''), IFNULL(area, ''), addressDetail) WHERE province IS NULL;
