-- mooar-market 建表脚本
-- 数据库: mooar-market

CREATE TABLE IF NOT EXISTS item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  type INT NOT NULL COMMENT '1=ERC20(GMT) 2=ERC1155(box) 3=ERC721(badge)',
  contract VARCHAR(64) NOT NULL DEFAULT '',
  token_id BIGINT NOT NULL DEFAULT 0 COMMENT '1155 的 tokenId，box=1',
  chain_scale BIGINT NOT NULL DEFAULT 1 COMMENT '1 个系统单位对应的链上最小单位数，GMT=10^16',
  image_prefix VARCHAR(128) NOT NULL DEFAULT '',
  state INT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fid VARCHAR(64) NOT NULL DEFAULT '' COMMENT '第三方id，扫块先建空 fid，登录回填',
  email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  address VARCHAR(64) NOT NULL COMMENT '钱包地址(小写)',
  state INT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_address (address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  available BIGINT NOT NULL DEFAULT 0 COMMENT '可用(系统单位)',
  frozen BIGINT NOT NULL DEFAULT 0 COMMENT '冻结(系统单位)',
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_item (user_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  contract VARCHAR(64) NOT NULL,
  token_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL DEFAULT 0,
  state INT NOT NULL DEFAULT 0 COMMENT '0可用 1挂单中 2提现中',
  order_id BIGINT NOT NULL DEFAULT 0,
  metadata TEXT COMMENT 'nft 属性 JSON',
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract_token (contract, token_id),
  KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  nft_id BIGINT NOT NULL DEFAULT 0 COMMENT '721 挂单用，0 表示同质化资产',
  amount BIGINT NOT NULL DEFAULT 0 COMMENT '同质化数量(系统单位)',
  price BIGINT NOT NULL COMMENT '单价 GMT(系统单位)',
  state INT NOT NULL DEFAULT 0 COMMENT '0挂单中 1已成交 2已取消',
  buyer_id BIGINT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  KEY idx_item_price (item_id, price),
  KEY idx_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trade (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  nft_id BIGINT NOT NULL DEFAULT 0,
  amount BIGINT NOT NULL DEFAULT 0,
  price BIGINT NOT NULL,
  total BIGINT NOT NULL COMMENT '总价 GMT(系统单位)',
  fee BIGINT NOT NULL DEFAULT 0 COMMENT '手续费(GMT系统单位)',
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  created_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS withdraw (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  nft_id BIGINT NOT NULL DEFAULT 0,
  amount BIGINT NOT NULL DEFAULT 0 COMMENT '同质化数量(系统单位)',
  fee BIGINT NOT NULL DEFAULT 0 COMMENT '手续费(GMT系统单位)',
  to_addr VARCHAR(64) NOT NULL,
  tx_hash VARCHAR(128) NOT NULL DEFAULT '',
  state INT NOT NULL DEFAULT 0 COMMENT '0待上链 1广播中 2成功 3失败 4超限人工',
  retry_count INT NOT NULL DEFAULT 0,
  nonce BIGINT NOT NULL DEFAULT -1,
  err_msg VARCHAR(256) NOT NULL DEFAULT '',
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  KEY idx_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS balance_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  change_amount BIGINT NOT NULL COMMENT '变动(系统单位, 可正可负)',
  balance_after BIGINT NOT NULL DEFAULT 0,
  biz_type INT NOT NULL COMMENT '1充值 2挂单冻结 3成交 4取消解冻 5提现 6手续费',
  ref_id BIGINT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chain_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  address VARCHAR(64) NOT NULL,
  encrypted_private_key VARCHAR(512) NOT NULL DEFAULT '',
  chain_id BIGINT NOT NULL DEFAULT 0,
  state INT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_address (address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS deposit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tx_hash VARCHAR(128) NOT NULL,
  log_index INT NOT NULL,
  block_number BIGINT NOT NULL DEFAULT 0 COMMENT '块号',
  block_time BIGINT NOT NULL DEFAULT 0 COMMENT '块时间戳(秒)',
  from_addr VARCHAR(64) NOT NULL DEFAULT '' COMMENT '转出地址',
  to_addr VARCHAR(64) NOT NULL DEFAULT '' COMMENT '转入地址(主地址)',
  contract VARCHAR(64) NOT NULL DEFAULT '' COMMENT '合约地址',
  token_id BIGINT NOT NULL DEFAULT 0 COMMENT 'tokenId，ERC20为0',
  amount VARCHAR(64) NOT NULL DEFAULT '' COMMENT '原始数量(wei字符串/整数)',
  json_data TEXT COMMENT '原始事件完整信息(JSON)',
  created_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tx_log (tx_hash, log_index),
  KEY idx_block (block_number),
  KEY idx_from (from_addr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_kv (
  k VARCHAR(64) PRIMARY KEY,
  v VARCHAR(256) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置道具
INSERT INTO item (name, type, contract, token_id, chain_scale, image_prefix) VALUES
  ('GMT', 1, '0x714DB550b574b3E927af3D93E26127D15721D4C2', 0, 10000000000000000, 'http://mooar-market/'),
  ('box', 2, '0x74796fd8c0ef5c17340f38f5ff672580d3e57f2d', 1, 1, 'http://mooar-market/'),
  ('badge', 3, '0x462DD5C6A786aCB47c60ad419FeaB11054B0b249', 0, 1, 'http://mooar-market/')
ON DUPLICATE KEY UPDATE name = VALUES(name);
