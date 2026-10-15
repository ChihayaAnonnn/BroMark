CREATE TABLE IF NOT EXISTS id_block (name varchar(32) PRIMARY KEY,next_value bigint NOT NULL CHECK(next_value>0)) ENGINE=InnoDB;
INSERT IGNORE INTO id_block VALUES('global',1);
