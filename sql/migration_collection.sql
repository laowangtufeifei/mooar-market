-- collection 表 + item.collection_id（按合约把同一系列的 item 归到一个 collection）
-- 可重复执行：建表用 IF NOT EXISTS，加列用 information_schema 判断，回填 collection 用 ON DUPLICATE KEY

-- ① item 加归属字段
SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE item ADD COLUMN collection_id BIGINT NOT NULL DEFAULT 0 COMMENT ''所属 collection，0=未归属'' AFTER traits_json, ADD KEY idx_collection (collection_id)',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item' AND COLUMN_NAME = 'collection_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② collection 表
CREATE TABLE IF NOT EXISTS collection (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract VARCHAR(64) NOT NULL COMMENT '链上合约地址，唯一',
  name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '系列名',
  cover VARCHAR(256) NOT NULL DEFAULT '' COMMENT '系列封面，和 tokenId 级图片无关',
  description TEXT COMMENT '系列描述',
  sort INT NOT NULL DEFAULT 0 COMMENT '列表排序，小的在前',
  state INT NOT NULL DEFAULT 0 COMMENT '0=正常 1=隐藏',
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract (contract)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ③ 每个合约建一个 collection，名称/封面先取「同合约下 id 最小那行」的值，之后手工换成系列封面
INSERT INTO collection (contract, name, cover, created_at, updated_at)
SELECT LOWER(t.contract), t.name, t.image_prefix,
       UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000
FROM item t
JOIN (
  SELECT LOWER(contract) AS c, MIN(id) AS mid
  FROM item
  WHERE type <> 1 AND contract <> ''
  GROUP BY LOWER(contract)
) g ON t.id = g.mid
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

-- ④ 回填 item.collection_id
UPDATE item i
JOIN collection c ON LOWER(i.contract) = c.contract
SET i.collection_id = c.id
WHERE i.collection_id = 0 AND i.contract <> '';
