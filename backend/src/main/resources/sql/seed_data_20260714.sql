-- ============================================================
-- 演示种子数据 2026-07-14（商品 / 头像 / 知识文章）
-- 说明：
--   * 与 agri_platform.sql 互相独立，只跑这一个文件即可。
--   * 全部为「增量 + 幂等」：可重复执行，不会删除或覆盖你已创建的数据。
--   * 商品仅在「同标题不存在」时插入；头像仅在「avatar 为空」时补；知识仅在「同标题不存在」时插入。
-- 执行：USE agri_platform; SOURCE src/main/resources/sql/seed_data_20260714.sql;
-- 图片需已存在于 frontend/public/images/（文件名含中文，浏览器会自动编码）。
-- ============================================================
USE agri_platform;

-- 取一个已存在的农户作为商品发布人，没有则回退到 farmer01
SELECT COALESCE((SELECT user_name FROM tb_user WHERE role = 'farmer' ORDER BY user_name LIMIT 1), 'farmer01') INTO @farmer;
-- 取一个已存在的专家作为知识作者，没有则回退到 expert01
SELECT COALESCE((SELECT user_name FROM tb_user WHERE role = 'expert' ORDER BY user_name LIMIT 1), 'expert01') INTO @expert;

-- ============================ 商品（货源） ============================
INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '攀枝花天然有机芒果', 39.90, '当季新鲜采摘，自然成熟，果肉细腻、香甜多汁，全程有机种植。', '/images/天然有机芒果.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '攀枝花天然有机芒果');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '山东牛奶小芋头', 19.90, '山东沙地产小芋头，口感软糯如牛奶般顺滑，香糯无筋。', '/images/山东牛奶小芋头.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '山东牛奶小芋头');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '无锡阳山水蜜桃', 99.00, '国家地理标志产品，皮薄多汁、入口即化，99元起整箱包邮。', '/images/水蜜桃（99元起）.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '无锡阳山水蜜桃');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '福建平和蜜柚', 29.90, '福建平和琯溪蜜柚，皮薄肉厚、清甜微酸，富含维C。', '/images/福建蜜柚.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '福建平和蜜柚');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '吐鲁番无核白葡萄', 79.00, '新疆吐鲁番直供，无核、皮薄、高糖度，79元整箱。', '/images/吐鲁番葡萄（79元）.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '吐鲁番无核白葡萄');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '农家手工地瓜粉', 25.00, '传统手工制作的红薯粉条，久煮不烂、爽滑筋道。', '/images/农家地瓜粉.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '农家手工地瓜粉');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '东北鸡心海棠果', 35.00, '东北特产鸡心海棠果，酸甜可口、果形似鸡心，开胃佳品。', '/images/东北鸡心海棠果.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '东北鸡心海棠果');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '内蒙古黄心土豆', 12.90, '沙土地黄心马铃薯，淀粉含量高、口感粉糯，耐储存。', '/images/土豆.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '内蒙古黄心土豆');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '东北鲜香菇', 18.80, '东北产区新鲜香菇，肉厚柄短、香气浓郁，现采现发。', '/images/小蘑菇.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '东北鲜香菇');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '陕西高山青苹果', 22.50, '陕西高原青苹果，脆爽酸甜、果香浓郁，耐放不易发面。', '/images/青苹果.jpg', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '陕西高山青苹果');

INSERT INTO tb_product (title, price, content, picture, type, order_status, own_name, create_time, update_time)
SELECT '东北五常稻花香大米', 59.90, '五常核心产区稻花香大米，米饭油润、剩饭不回生，10斤装。', '/images/稻米.webp', 'goods', 0, @farmer, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_product WHERE title = '东北五常稻花香大米');

-- ============================ 用户头像（仅补空） ============================
UPDATE tb_user SET avatar = '/images/(专家)头像4.png'
 WHERE role = 'expert' AND (avatar IS NULL OR avatar = '');
UPDATE tb_user SET avatar = '/images/头像2.png'
 WHERE role = 'bank' AND (avatar IS NULL OR avatar = '');
UPDATE tb_user SET avatar = '/images/头像3.jpg'
 WHERE role = 'admin' AND (avatar IS NULL OR avatar = '');
UPDATE tb_user SET avatar = '/images/头像1.png'
 WHERE role IN ('farmer', 'buyer') AND (avatar IS NULL OR avatar = '');

-- ============================ 知识文章（可选，填充知识库） ============================
INSERT INTO tb_knowledge (title, content, pic_path, own_name, status, create_time, update_time)
SELECT '芒果高产种植技术与常见病虫害防治',
       '<p>芒果喜温好光，花期注意防寒保花；坐果期合理疏果，防治炭疽病与蓟马。</p>',
       '/images/天然有机芒果.jpg', @expert, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_knowledge WHERE title = '芒果高产种植技术与常见病虫害防治');

INSERT INTO tb_knowledge (title, content, pic_path, own_name, status, create_time, update_time)
SELECT '马铃薯（土豆）安全储藏与防发芽技巧',
       '<p>储藏前晾晒去泥，保持低温（3-5℃）、避光、通风；可与苹果同放延缓发芽。</p>',
       '/images/土豆.jpg', @expert, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_knowledge WHERE title = '马铃薯（土豆）安全储藏与防发芽技巧');

INSERT INTO tb_knowledge (title, content, pic_path, own_name, status, create_time, update_time)
SELECT '水稻田间管理与水分调控要点',
       '<p>分蘖期浅水促蘖，拔节孕穗期保持水层，抽穗后干湿交替，成熟期适时断水。</p>',
       '/images/稻米.webp', @expert, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_knowledge WHERE title = '水稻田间管理与水分调控要点');
