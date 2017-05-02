DROP TABLE IF EXISTS app;
CREATE TABLE app (
  `id` INT(12) PRIMARY KEY,
  `name` VARCHAR(80),
  `key` VARCHAR(80) COMMENT 'client to server key',
  `secret` VARCHAR(80) COMMENT 'server to server key',
  `status` INT(1) DEFAULT 0 COMMENT '0=allow,1=deny',
  `pay_type` INT(8) DEFAULT 0 COMMENT '0x00=none 0x01=google&apple 0x02=mycard&apple 0x03=paypal&apple 0x04=google&mycard&apple 0x05=google&paypal&apple 0x06=google&mycard&paypal&apple 0x07=mycard&paypal&apple',
  `notify_url` VARCHAR(200),
  `create_time` DATETIME
) DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS user_base;
CREATE TABLE user_base (
  `id` BIGINT(20) PRIMARY KEY,
  `name` VARCHAR(40) NOT NULL,
  `password` VARCHAR(32) NOT NULL ,
  `email` VARCHAR(40) DEFAULT NULL,
  `status` INT(1) DEFAULT 0 COMMENT '0=allow 1=deny',
  `create_time` DATETIME,
  UNIQUE KEY (`name`)
) DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS user_channel;
CREATE TABLE user_channel (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT(20),
  `channel_id` VARCHAR(60) NOT NULL,
  `channel_type` INT(1) DEFAULT 1 COMMENT '0seastar, 1guest 2google 3gamecenter 4facebook',
  `create_time` datetime,
  UNIQUE KEY (`channel_id`, `channel_type`),
  KEY (`user_id`)
) DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS sku;
CREATE TABLE sku (
  `id` INT(20) PRIMARY KEY AUTO_INCREMENT,
  `app_id` INT(12),
  `sku` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `price` VARCHAR(24) NOT NULL,
  `currency` VARCHAR(8) NOT NULL,
  `platform` INT(2) DEFAULT 0 COMMENT '0-none 1-google 2-apple 3-mycard 4-paypal',
  `create_time` DATETIME,
  UNIQUE KEY(`app_id`, `sku`),
  KEY(`app_id`)
) DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS pay_info;
CREATE TABLE pay_info (
  `id` INT(20) PRIMARY KEY AUTO_INCREMENT,
  `order` VARCHAR(60) NOT NULL,
  `app_id` INT(12) DEFAULT 0,
  `user_id` BIGINT(20) DEFAULT 0,
  `customer_id` VARCHAR(48) DEFAULT NULL,
  `server_id` VARCHAR(16) DEFAULT NULL,
  `status` INT(1) DEFAULT 0 COMMENT '0-push success 1=push fail',
  `sku` VARCHAR(64),
  `price` VARCHAR(24),
  `currency` VARCHAR(8),
  `currency_used` VARCHAR (8),
  `channel_type` INT(8) DEFAULT 0 COMMENT '0-none 1=google 2=apple 3=mycard 4=paypal',
  `channel_order` VARCHAR(60),
  `create_time` datetime,
  `notify_time` datetime DEFAULT NULL,
  `sandbox` INT(1) DEFAULT 0 COMMENT '0-production，1-sandbox',
  `extra` VARCHAR(200) DEFAULT NULL,
  UNIQUE KEY (`order`),
  KEY (`channel_order`, `channel_type`)
) DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS app_google;
CREATE TABLE app_google (
  `id` INT(12) PRIMARY KEY ,
  `key` VARCHAR(1024) NOT NULL
) DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS mycard_trade;
CREATE TABLE mycard_trade (
  `id` int(10) PRIMARY KEY AUTO_INCREMENT,
  `payment_type` varchar(15) NOT NULL COMMENT 'mycard付费方式, INGAME实体卡，COSTPOINT点卡 FA018上海webatm, FA029中华电信HiNet连扣, FA200000002测试用',
  `trade_seq` varchar(60) DEFAULT NULL COMMENT '充值渠道订单',
  `mycard_trade_no` varchar(60) DEFAULT NULL ,
  `fac_trade_seq` varchar(60) DEFAULT NULL ,
  `customer_id` VARCHAR (60) DEFAULT NULL,
  `amount` varchar(60) DEFAULT '0' COMMENT '交易金额，可以为整数，若有小鼠最多2位',
  `currency` varchar(10) DEFAULT 'TWD' COMMENT '货币单位',
  `trade_date_time` datetime DEFAULT NULL,
  KEY (`mycard_trade_no`)
) DEFAULT CHARSET=utf8;
/*
DROP TABLE IF EXISTS auth_user;
CREATE TABLE auth_user (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `password` varchar(128) NOT NULL,
  `username` varchar(150) NOT NULL,
  `auth` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS operative;
CREATE TABLE operative (
  id INT(20) PRIMARY KEY AUTO_INCREMENT,
  appId int(12) DEFAULT 0 COMMENT '应用id',
  productId varchar(32) NOT NULL COMMENT '商品id',
  virtualCoin varchar(10) DEFAULT '0' COMMENT '实际附送金额',
  giveVirtualCoin varchar(10) DEFAULT '0' COMMENT '赠送金额',
  money varchar(30) DEFAULT '0' COMMENT '默认币种下的金额',
  UNIQUE KEY (appId, productId)
) DEFAULT CHARSET=utf8;
*/