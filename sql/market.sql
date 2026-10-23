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
  traits_json TEXT COMMENT 'trait 槽位定义 JSON（槽位↔属性名映射 + 值编码表，per-collection）',
  collection_id BIGINT NOT NULL DEFAULT 0 COMMENT '所属 collection.id，0=未归属',
  state INT NOT NULL DEFAULT 0 COMMENT '0=不扫块 1=扫块',
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  KEY idx_collection (collection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fid VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'FSL 用户 id，扫块先建空 fid，登录回填',
  name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '昵称',
  avatar VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像',
  email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  stepn_email VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'STEPN 邮箱',
  address VARCHAR(64) NULL DEFAULT NULL COMMENT '钱包地址(小写)，登录时可能还没有',
  state INT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_address (address),
  KEY idx_fid (fid)
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
  image VARCHAR(128) DEFAULT NULL COMMENT 'nft 图片',
  metadata TEXT COMMENT 'nft 原始 metadata JSON',
  level INT NOT NULL DEFAULT 0 COMMENT '等级(专用列)',
  v1 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位1',
  v2 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位2',
  v3 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位3',
  v4 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位4',
  v5 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位5',
  v6 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位6',
  v7 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位7',
  v8 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位8',
  v9 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位9',
  v10 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位10',
  v11 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位11',
  v12 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位12',
  v13 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位13',
  v14 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位14',
  v15 INT NOT NULL DEFAULT 0 COMMENT 'trait 槽位15',
  str VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'trait 兜底',
  created_at BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract_token (contract, token_id),
  KEY idx_owner (owner_id),
  KEY idx_item_state (item_id, state),
  KEY idx_item_level (item_id, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 存量库升级用（新库执行上面的 CREATE 即可，无需下面语句）
-- ALTER TABLE item ADD COLUMN traits_json TEXT COMMENT 'trait 槽位定义 JSON';
-- ALTER TABLE nft
--   ADD COLUMN image VARCHAR(128) DEFAULT NULL COMMENT 'nft 图片',
--   ADD COLUMN level INT NOT NULL DEFAULT 0,
--   ADD COLUMN v1 INT NOT NULL DEFAULT 0, ADD COLUMN v2 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v3 INT NOT NULL DEFAULT 0, ADD COLUMN v4 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v5 INT NOT NULL DEFAULT 0, ADD COLUMN v6 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v7 INT NOT NULL DEFAULT 0, ADD COLUMN v8 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v9 INT NOT NULL DEFAULT 0, ADD COLUMN v10 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v11 INT NOT NULL DEFAULT 0, ADD COLUMN v12 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v13 INT NOT NULL DEFAULT 0, ADD COLUMN v14 INT NOT NULL DEFAULT 0,
--   ADD COLUMN v15 INT NOT NULL DEFAULT 0,
--   ADD COLUMN str VARCHAR(255) NOT NULL DEFAULT '',
--   ADD KEY idx_item_state (item_id, state), ADD KEY idx_item_level (item_id, level);

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
  created_at BIGINT NOT NULL DEFAULT 0,
  KEY idx_created_item (created_at, item_id)
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

-- 预置道具（traits_json = 该 collection 的筛选槽位定义：槽位↔属性名 + 值编码表）
INSERT INTO item (id,name,type,contract,token_id,chain_scale,image_prefix,traits_json) VALUES
(1,'GMT',1,'0x714DB550b574b3E927af3D93E26127D15721D4C2',0,10000000000000000,'http://mooar-market/',NULL),
(2,'box',2,'0x74796fd8c0ef5c17340f38f5ff672580d3e57f2d',1,1,'http://mooar-market/','{"traits":[{"name":"State","slot":"v1","type":"enum","values":{"Active":1}}]}'),
(3,'badge',3,'0x462DD5C6A786aCB47c60ad419FeaB11054B0b249',0,1,'http://mooar-market/','{"traits":[{"name":"Background","slot":"v1","type":"enum","values":{"80s Horizontal Blue (Common)":1,"80s Horizontal Cyan (Common)":2,"80s Horizontal Green (Common)":3,"80s Horizontal Grey (Common)":4,"80s Horizontal Red (Common)":5,"80s Horizontal Yellow (Common)":6,"ADL Purple (Common)":7,"Error Blue (Legendary)":8,"Error Green (Legendary)":9,"Error Grey (Legendary)":10,"Error Orange (Legendary)":11,"Error Red (Legendary)":12,"Error Yellow (Legendary)":13,"Game Blue (Common)":14,"Game Green (Common)":15,"Game Grey (Common)":16,"Game Purple (Common)":17,"Game Yellow (Common)":18,"Halloween Purple (Common)":19,"Pixel Blue (Rare)":20,"Pixel Green (Rare)":21,"Pixel Grey (Rare)":22,"Pixel Red (Rare)":23,"Pixel Yellow (Rare)":24}},{"name":"Body","slot":"v2","type":"enum","values":{"Marty Armour Blue (Rare)":1,"Marty Armour Green (Rare)":2,"Marty Armour Purple (Rare)":3,"Marty Armour Yellow (Rare)":4,"Marty Flameset Blue (Legendary)":5,"Marty Flameset Purple (Legendary)":6,"Marty Flameset Red (Legendary)":7,"Marty Flameset Yellow (Legendary)":8,"Marty STEPN Blue (Common)":9,"Marty STEPN Green (Common)":10,"Marty STEPN Grey (Common)":11,"Marty STEPN Red (Common)":12,"Marty VR Pink (Common)":13,"Marty VR Purple (Common)":14,"Marty VR Red (Common)":15,"Marty VR Yellow (Common)":16,"Miffy Armour Blue (Rare)":17,"Miffy Armour Green (Rare)":18,"Miffy Armour Purple (Rare)":19,"Miffy Armour Yellow (Rare)":20,"Miffy Flameset Blue (Legendary)":21,"Miffy Flameset Purple (Legendary)":22,"Miffy Flameset Red (Legendary)":23,"Miffy Flameset Yellow (Legendary)":24,"Miffy STEPN Blue (Common)":25,"Miffy STEPN Green (Common)":26,"Miffy STEPN Purple (Common)":27,"Miffy STEPN Red (Common)":28,"Miffy VR Blue (Common)":29,"Miffy VR Green (Common)":30,"Miffy VR Pink (Common)":31,"Miffy VR Red (Common)":32,"Mog Armour Blue (Rare)":33,"Mog Armour Green (Rare)":34,"Mog Armour Purple (Rare)":35,"Mog Armour Yellow (Rare)":36,"Mog Flameset Blue (Legendary)":37,"Mog Flameset Purple (Legendary)":38,"Mog Flameset Red (Legendary)":39,"Mog Flameset Yellow (Legendary)":40,"Mog STEPN Blue (Common)":41,"Mog STEPN Green (Common)":42,"Mog STEPN Orange (Common)":43,"Mog STEPN Pink (Common)":44,"Mog VR Blue (Common)":45,"Mog VR Green (Common)":46,"Mog VR Orange (Common)":47,"Mog VR Pink (Common)":48,"Mooarty ADL Purple (Common)":49,"Mooarty Armour Blue (Rare)":50,"Mooarty Armour Green (Rare)":51,"Mooarty Armour Purple (Rare)":52,"Mooarty Armour Red (Rare)":53,"Mooarty Flameset Blue (Legendary)":54,"Mooarty Flameset Purple (Legendary)":55,"Mooarty Flameset Red (Legendary)":56,"Mooarty Flameset Yellow (Legendary)":57,"Mooarty Mid-Autumn Orange (Common)":58,"Mooarty STEPN Blue (Common)":59,"Mooarty STEPN Brown (Common)":60,"Mooarty STEPN Grey (Common)":61,"Mooarty STEPN Purple (Common)":62,"Mooarty VR Blue (Common)":63,"Mooarty VR Green (Common)":64,"Mooarty VR Purple (Common)":65,"Mooarty VR Yellow (Common)":66,"Mooarty Vampire Black (Common)":67,"Mooarty Zombie Green (Common)":68}},{"name":"Character","slot":"v3","type":"enum","values":{"Marty":1,"Miffy":2,"Mog":3,"Mooarty":4}},{"name":"Headgear","slot":"v4","type":"enum","values":{"Marty Armour Blue (Rare)":1,"Marty Armour Green (Rare)":2,"Marty Armour Purple (Rare)":3,"Marty Armour Yellow (Rare)":4,"Marty Flameset Blue (Legendary)":5,"Marty Flameset Green (Legendary)":6,"Marty Flameset Purple (Legendary)":7,"Marty Flameset Yellow (Legendary)":8,"Marty STEPN Blue (Common)":9,"Marty STEPN Green (Common)":10,"Marty STEPN Grey (Common)":11,"Marty STEPN Red (Common)":12,"Marty VR Pink (Common)":13,"Marty VR Purple (Common)":14,"Marty VR Red (Common)":15,"Marty VR Yellow (Common)":16,"Miffy Armour Blue (Rare)":17,"Miffy Armour Green (Rare)":18,"Miffy Armour Purple (Rare)":19,"Miffy Armour Yellow (Rare)":20,"Miffy Flameset Blue (Legendary)":21,"Miffy Flameset Orange (Legendary)":22,"Miffy Flameset Purple (Legendary)":23,"Miffy Flameset Red (Legendary)":24,"Miffy STEPN Blue (Common)":25,"Miffy STEPN Green (Common)":26,"Miffy STEPN Purple (Common)":27,"Miffy STEPN Red (Common)":28,"Miffy VR Blue (Common)":29,"Miffy VR Green (Common)":30,"Miffy VR Pink (Common)":31,"Miffy VR Red (Common)":32,"Mog Armour Blue (Rare)":33,"Mog Armour Green (Rare)":34,"Mog Armour Purple (Rare)":35,"Mog Armour Yellow (Rare)":36,"Mog Flameset Blue (Legendary)":37,"Mog Flameset Orange (Legendary)":38,"Mog Flameset Purple (Legendary)":39,"Mog Flameset Red (Legendary)":40,"Mog STEPN Blue (Common)":41,"Mog STEPN Green (Common)":42,"Mog STEPN Pink (Common)":43,"Mog STEPN Red (Common)":44,"Mog VR Blue (Common)":45,"Mog VR Green (Common)":46,"Mog VR Orange (Common)":47,"Mog VR Pink (Common)":48,"Mooarty ADL Purple (Common)":49,"Mooarty Armour Blue (Rare)":50,"Mooarty Armour Green (Rare)":51,"Mooarty Armour Purple (Rare)":52,"Mooarty Armour Yellow (Rare)":53,"Mooarty Flameset Blue (Legendary)":54,"Mooarty Flameset Green (Legendary)":55,"Mooarty Flameset Orange (Legendary)":56,"Mooarty Flameset Purple (Legendary)":57,"Mooarty Flameset Red (Legendary)":58,"Mooarty Mid-Autumn Orange (Common)":59,"Mooarty STEPN Blue (Common)":60,"Mooarty STEPN Grey (Common)":61,"Mooarty STEPN Purple (Common)":62,"Mooarty STEPN Yellow (Common)":63,"Mooarty VR Green (Common)":64,"Mooarty VR Grey (Common)":65,"Mooarty VR Purple (Common)":66,"Mooarty VR Red (Common)":67,"Mooarty Vampire Black (Common)":68,"Mooarty Zombie Green (Common)":69}},{"name":"Rarity","slot":"v5","type":"enum","values":{"Common":1,"Legendary":2,"Rare":3}},{"name":"Sneaker","slot":"v6","type":"enum","values":{"Marty Armour Blue (Rare)":1,"Marty Armour Green (Rare)":2,"Marty Armour Purple (Rare)":3,"Marty Armour Yellow (Rare)":4,"Marty Flameset Blue (Legendary)":5,"Marty Flameset Orange (Legendary)":6,"Marty Flameset Purple (Legendary)":7,"Marty Flameset Red (Legendary)":8,"Marty STEPN Blue (Common)":9,"Marty STEPN Green (Common)":10,"Marty STEPN Red (Common)":11,"Marty STEPN Yellow (Common)":12,"Marty VR Pink (Common)":13,"Marty VR Purple (Common)":14,"Marty VR Red (Common)":15,"Marty VR Yellow (Common)":16,"Miffy Armour Blue (Rare)":17,"Miffy Armour Green (Rare)":18,"Miffy Armour Purple (Rare)":19,"Miffy Armour Yellow (Rare)":20,"Miffy Flameset Blue (Legendary)":21,"Miffy Flameset Orange (Legendary)":22,"Miffy Flameset Purple (Legendary)":23,"Miffy Flameset Red (Legendary)":24,"Miffy STEPN Blue (Common)":25,"Miffy STEPN Green (Common)":26,"Miffy STEPN Orange (Common)":27,"Miffy STEPN Red (Common)":28,"Miffy VR Blue (Common)":29,"Miffy VR Green (Common)":30,"Miffy VR Pink (Common)":31,"Miffy VR Red (Common)":32,"Mog Armour Blue (Rare)":33,"Mog Armour Green (Rare)":34,"Mog Armour Purple (Rare)":35,"Mog Armour Yellow (Rare)":36,"Mog Flameset Blue (Legendary)":37,"Mog Flameset Purple (Legendary)":38,"Mog Flameset Red (Legendary)":39,"Mog Flameset Yellow (Legendary)":40,"Mog STEPN Blue (Common)":41,"Mog STEPN Green (Common)":42,"Mog STEPN Pink (Common)":43,"Mog STEPN Red (Common)":44,"Mog VR Blue (Common)":45,"Mog VR Green (Common)":46,"Mog VR Orange (Common)":47,"Mog VR Pink (Common)":48,"Mooarty ADL Purple (Common)":49,"Mooarty Armour Blue (Rare)":50,"Mooarty Armour Purple (Rare)":51,"Mooarty Armour Red (Rare)":52,"Mooarty Armour Yellow (Rare)":53,"Mooarty Flamset Blue (Legendary)":54,"Mooarty Flamset Orange (Legendary)":55,"Mooarty Flamset Purple (Legendary)":56,"Mooarty Flamset Red (Legendary)":57,"Mooarty Mid-Autumn Orange (Common)":58,"Mooarty STEPN Blue (Common)":59,"Mooarty STEPN Green (Common)":60,"Mooarty STEPN Orange (Common)":61,"Mooarty STEPN Red (Common)":62,"Mooarty VR Blue (Common)":63,"Mooarty VR Green (Common)":64,"Mooarty VR Purple (Common)":65,"Mooarty VR Yellow (Common)":66,"Mooarty Vampire Black (Common)":67,"Mooarty Zombie Green (Common)":68}},{"name":"Sticker","slot":"v7","type":"enum","values":{"Marty Armour (Rare)":1,"Miffy Armour (Rare)":2,"Mog Armour (Rare)":3,"Mooarty Armour (Rare)":4}}]}'),
(4,'MORCHI',3,'0xB48f53010Acbc0E24D3D12D892E2215879e6fD13',0,1,'http://mooar-market/','{"traits":[{"name":"Rarity","slot":"v1","type":"enum","values":{"Common":1,"Legendary":2,"Rare":3}},{"name":"Type","slot":"v2","type":"enum","values":{"Genesis":1,"Invitation Mint":2}},{"name":"Level","slot":"level","type":"number","min":0,"max":100},{"name":"Character","slot":"v3","type":"enum","values":{"Iggy":1,"Marty":2,"Miffy":3,"Mog":4,"Mooarty":5,"Spesh":6}},{"name":"Device","slot":"v4","type":"enum","values":{"Angry Dino (Common)":1,"Blast (Common)":2,"Box Green (Common)":3,"Box Grey (Common)":4,"Box Purple (Common)":5,"Box Red (Common)":6,"Box Teal (Common)":7,"Box Yellow (Common)":8,"Controller Green (Legendary)":9,"Controller Grey (Legendary)":10,"Controller Purple (Legendary)":11,"Controller Red (Legendary)":12,"Controller Yellow (Legendary)":13,"Coolman Iggy White (Rare)":14,"Coolman Spesh Green (Rare)":15,"Disk Grey (Common)":16,"Disk Purple (Common)":17,"Disk Red (Common)":18,"Disk Teal (Common)":19,"Disk Yellow (Common)":20,"Famous Foxes (Common)":21,"Halloween Red (Common)":22,"Halloween Red (Legendary)":23,"Halloween Red (Rare)":24,"LOK (Common)":25,"Mid-Autumn Orange (Common)":26,"Plato (Common)":27,"Round Blue (Rare)":28,"Round Green (Rare)":29,"Round Grey (Rare)":30,"Round Purple (Rare)":31,"Round Teal (Rare)":32,"Top Goal (Common)":33}},{"name":"Screen","slot":"v5","type":"enum","values":{"80s Horizontal Blue (Common)":1,"80s Horizontal Cyan (Common)":2,"80s Horizontal Green (Common)":3,"80s Horizontal Grey (Common)":4,"80s Horizontal Red (Common)":5,"80s Horizontal Yellow (Common)":6,"ADL Purple (Common)":7,"Coolman Iggy Yellow (Rare)":8,"Coolman Spesh Yellow (Rare)":9,"Error Blue (Legendary)":10,"Error Green (Legendary)":11,"Error Grey (Legendary)":12,"Error Orange (Legendary)":13,"Error Red (Legendary)":14,"Error Yellow (Legendary)":15,"Game Blue (Common)":16,"Game Green (Common)":17,"Game Grey (Common)":18,"Game Purple (Common)":19,"Game Yellow (Common)":20,"Halloween Purple (Common)":21,"Pixel Blue (Rare)":22,"Pixel Green (Rare)":23,"Pixel Grey (Rare)":24,"Pixel Red (Rare)":25,"Pixel Yellow (Rare)":26}},{"name":"Background","slot":"v6","type":"enum","values":{"90s kid Green":1,"90s kid Orange":2,"90s kid Purple":3,"90s kid blue":4,"ADL":5,"BANX":6,"Coolman Coffedan":7,"Cyberpunk Green":8,"Cyberpunk Orange":9,"Cyberpunk Purple":10,"Cyberpunk Red":11,"DEFILAND":12,"HONEYLAND":13,"Halloween":14,"MATR1X":15,"Mid-Autumn":16,"PLUTO":17,"Psychedelic Green":18,"Psychedelic Purple":19,"Psychedelic Red":20,"Psychedelic blue":21,"RAGE EFFECT":22,"SPACE MAVERICKS":23,"Sticker V1":24,"Sticker V2":25,"Sticker V3":26,"Sticker V4":27}},{"name":"Body","slot":"v7","type":"enum","values":{"Iggy Coolman Iggy Grey (Rare)":1,"Marty Armour Blue (Rare)":2,"Marty Armour Green (Rare)":3,"Marty Armour Purple (Rare)":4,"Marty Armour Yellow (Rare)":5,"Marty Flameset Blue (Legendary)":6,"Marty Flameset Purple (Legendary)":7,"Marty Flameset Red (Legendary)":8,"Marty Flameset Yellow (Legendary)":9,"Marty STEPN Blue (Common)":10,"Marty STEPN Green (Common)":11,"Marty STEPN Grey (Common)":12,"Marty STEPN Red (Common)":13,"Marty VR Pink (Common)":14,"Marty VR Purple (Common)":15,"Marty VR Red (Common)":16,"Marty VR Yellow (Common)":17,"Miffy Armour Blue (Rare)":18,"Miffy Armour Green (Rare)":19,"Miffy Armour Purple (Rare)":20,"Miffy Armour Yellow (Rare)":21,"Miffy Flameset Blue (Legendary)":22,"Miffy Flameset Purple (Legendary)":23,"Miffy Flameset Red (Legendary)":24,"Miffy Flameset Yellow (Legendary)":25,"Miffy STEPN Blue (Common)":26,"Miffy STEPN Green (Common)":27,"Miffy STEPN Purple (Common)":28,"Miffy STEPN Red (Common)":29,"Miffy VR Blue (Common)":30,"Miffy VR Green (Common)":31,"Miffy VR Pink (Common)":32,"Miffy VR Red (Common)":33,"Mog Armour Blue (Rare)":34,"Mog Armour Green (Rare)":35,"Mog Armour Purple (Rare)":36,"Mog Armour Yellow (Rare)":37,"Mog Flameset Blue (Legendary)":38,"Mog Flameset Purple (Legendary)":39,"Mog Flameset Red (Legendary)":40,"Mog Flameset Yellow (Legendary)":41,"Mog STEPN Blue (Common)":42,"Mog STEPN Green (Common)":43,"Mog STEPN Orange (Common)":44,"Mog STEPN Pink (Common)":45,"Mog VR Blue (Common)":46,"Mog VR Green (Common)":47,"Mog VR Orange (Common)":48,"Mog VR Pink (Common)":49,"Mooarty ADL Purple (Common)":50,"Mooarty Armour Blue (Rare)":51,"Mooarty Armour Green (Rare)":52,"Mooarty Armour Purple (Rare)":53,"Mooarty Armour Red (Rare)":54,"Mooarty Flameset Blue (Legendary)":55,"Mooarty Flameset Purple (Legendary)":56,"Mooarty Flameset Red (Legendary)":57,"Mooarty Flameset Yellow (Legendary)":58,"Mooarty Mid-Autumn Orange (Common)":59,"Mooarty STEPN Blue (Common)":60,"Mooarty STEPN Brown (Common)":61,"Mooarty STEPN Grey (Common)":62,"Mooarty STEPN Purple (Common)":63,"Mooarty VR Blue (Common)":64,"Mooarty VR Green (Common)":65,"Mooarty VR Purple (Common)":66,"Mooarty VR Yellow (Common)":67,"Mooarty Vampire Black (Common)":68,"Mooarty Zombie Green (Common)":69,"Spesh Coolman Spesh Grey (Rare)":70}},{"name":"Headgear","slot":"v8","type":"enum","values":{"Iggy Coolman Iggy Pink (Rare)":1,"Marty Armour Blue (Rare)":2,"Marty Armour Green (Rare)":3,"Marty Armour Purple (Rare)":4,"Marty Armour Yellow (Rare)":5,"Marty Flameset Blue (Legendary)":6,"Marty Flameset Green (Legendary)":7,"Marty Flameset Purple (Legendary)":8,"Marty Flameset Yellow (Legendary)":9,"Marty STEPN Blue (Common)":10,"Marty STEPN Green (Common)":11,"Marty STEPN Grey (Common)":12,"Marty STEPN Red (Common)":13,"Marty VR Pink (Common)":14,"Marty VR Purple (Common)":15,"Marty VR Red (Common)":16,"Marty VR Yellow (Common)":17,"Miffy Armour Blue (Rare)":18,"Miffy Armour Green (Rare)":19,"Miffy Armour Purple (Rare)":20,"Miffy Armour Yellow (Rare)":21,"Miffy Flameset Blue (Legendary)":22,"Miffy Flameset Orange (Legendary)":23,"Miffy Flameset Purple (Legendary)":24,"Miffy Flameset Red (Legendary)":25,"Miffy STEPN Blue (Common)":26,"Miffy STEPN Green (Common)":27,"Miffy STEPN Purple (Common)":28,"Miffy STEPN Red (Common)":29,"Miffy VR Blue (Common)":30,"Miffy VR Green (Common)":31,"Miffy VR Pink (Common)":32,"Miffy VR Red (Common)":33,"Mog Armour Blue (Rare)":34,"Mog Armour Green (Rare)":35,"Mog Armour Purple (Rare)":36,"Mog Armour Yellow (Rare)":37,"Mog Flameset Blue (Legendary)":38,"Mog Flameset Orange (Legendary)":39,"Mog Flameset Purple (Legendary)":40,"Mog Flameset Red (Legendary)":41,"Mog STEPN Blue (Common)":42,"Mog STEPN Green (Common)":43,"Mog STEPN Pink (Common)":44,"Mog STEPN Red (Common)":45,"Mog VR Blue (Common)":46,"Mog VR Green (Common)":47,"Mog VR Orange (Common)":48,"Mog VR Pink (Common)":49,"Mooarty ADL Purple (Common)":50,"Mooarty Armour Blue (Rare)":51,"Mooarty Armour Green (Rare)":52,"Mooarty Armour Purple (Rare)":53,"Mooarty Armour Yellow (Rare)":54,"Mooarty Flameset Blue (Legendary)":55,"Mooarty Flameset Green (Legendary)":56,"Mooarty Flameset Orange (Legendary)":57,"Mooarty Flameset Purple (Legendary)":58,"Mooarty Flameset Red (Legendary)":59,"Mooarty Mid-Autumn Orange (Common)":60,"Mooarty STEPN Blue (Common)":61,"Mooarty STEPN Grey (Common)":62,"Mooarty STEPN Purple (Common)":63,"Mooarty STEPN Yellow (Common)":64,"Mooarty VR Green (Common)":65,"Mooarty VR Grey (Common)":66,"Mooarty VR Purple (Common)":67,"Mooarty VR Red (Common)":68,"Mooarty Vampire Black (Common)":69,"Mooarty Zombie Green (Common)":70,"Spesh Coolman Spesh Pink (Rare)":71}},{"name":"Sneaker","slot":"v9","type":"enum","values":{"Iggy Coolman Iggy Pink (Rare)":1,"Marty Armour Blue (Rare)":2,"Marty Armour Green (Rare)":3,"Marty Armour Purple (Rare)":4,"Marty Armour Yellow (Rare)":5,"Marty Flameset Blue (Legendary)":6,"Marty Flameset Orange (Legendary)":7,"Marty Flameset Purple (Legendary)":8,"Marty Flameset Red (Legendary)":9,"Marty STEPN Blue (Common)":10,"Marty STEPN Green (Common)":11,"Marty STEPN Red (Common)":12,"Marty STEPN Yellow (Common)":13,"Marty VR Pink (Common)":14,"Marty VR Purple (Common)":15,"Marty VR Red (Common)":16,"Marty VR Yellow (Common)":17,"Miffy Armour Blue (Rare)":18,"Miffy Armour Green (Rare)":19,"Miffy Armour Purple (Rare)":20,"Miffy Armour Yellow (Rare)":21,"Miffy Flameset Blue (Legendary)":22,"Miffy Flameset Orange (Legendary)":23,"Miffy Flameset Purple (Legendary)":24,"Miffy Flameset Red (Legendary)":25,"Miffy STEPN Blue (Common)":26,"Miffy STEPN Green (Common)":27,"Miffy STEPN Orange (Common)":28,"Miffy STEPN Red (Common)":29,"Miffy VR Blue (Common)":30,"Miffy VR Green (Common)":31,"Miffy VR Pink (Common)":32,"Miffy VR Red (Common)":33,"Mog Armour Blue (Rare)":34,"Mog Armour Green (Rare)":35,"Mog Armour Purple (Rare)":36,"Mog Armour Yellow (Rare)":37,"Mog Flameset Blue (Legendary)":38,"Mog Flameset Purple (Legendary)":39,"Mog Flameset Red (Legendary)":40,"Mog Flameset Yellow (Legendary)":41,"Mog STEPN Blue (Common)":42,"Mog STEPN Green (Common)":43,"Mog STEPN Pink (Common)":44,"Mog STEPN Red (Common)":45,"Mog VR Blue (Common)":46,"Mog VR Green (Common)":47,"Mog VR Orange (Common)":48,"Mog VR Pink (Common)":49,"Mooarty ADL Purple (Common)":50,"Mooarty Armour Blue (Rare)":51,"Mooarty Armour Purple (Rare)":52,"Mooarty Armour Red (Rare)":53,"Mooarty Armour Yellow (Rare)":54,"Mooarty Flamset Blue (Legendary)":55,"Mooarty Flamset Orange (Legendary)":56,"Mooarty Flamset Purple (Legendary)":57,"Mooarty Flamset Red (Legendary)":58,"Mooarty Mid-Autumn Orange (Common)":59,"Mooarty STEPN Blue (Common)":60,"Mooarty STEPN Green (Common)":61,"Mooarty STEPN Orange (Common)":62,"Mooarty STEPN Red (Common)":63,"Mooarty VR Blue (Common)":64,"Mooarty VR Green (Common)":65,"Mooarty VR Purple (Common)":66,"Mooarty VR Yellow (Common)":67,"Mooarty Vampire Black (Common)":68,"Mooarty Zombie Green (Common)":69,"Spesh Coolman Spesh Pink (Rare)":70}},{"name":"Sticker","slot":"v10","type":"enum","values":{"Marty Armour (Rare)":1,"Miffy Armour (Rare)":2,"Mog Armour (Rare)":3,"Mooarty Armour (Rare)":4}},{"name":"State","slot":"v11","type":"enum","values":{"Unlock":1}}]}'),
(5,'sneaker',3,'0x5b00646169FBc1487C472F1dC28de09C5d350Ced',0,1,'http://mooar-market/','{"traits":[{"name":"Rate Up","slot":"v1","type":"enum","values":{"100%":1,"150%":2,"50%":3,"No":4}},{"name":"Tradable","slot":"v2","type":"enum","values":{"No":1,"Yes":2}}]}'),
(6,'event',3,'0xA80739520Dfd78c4d4DFCb677Bb3877F93fe6AcA',0,1,'http://mooar-market/','{"traits":[{"name":"Event","slot":"v1","type":"enum","values":{"BidMOOAR":1,"Easter Egg Hunt":2,"Energy War":3,"FSL Festive Points Chase":4,"Halloween GGBox":5}}]}')
ON DUPLICATE KEY UPDATE traits_json=VALUES(traits_json), name=VALUES(name);
