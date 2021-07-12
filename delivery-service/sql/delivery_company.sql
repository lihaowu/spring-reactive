CREATE TABLE IF NOT EXISTS `delivery_company`(
   `id` INT UNSIGNED AUTO_INCREMENT,
   `name` VARCHAR(32) NOT NULL,
   `label` VARCHAR(64) ,
   `level` int default -1,
   UNIQUE KEY `name` (`name`),
   PRIMARY KEY ( `id` )
);