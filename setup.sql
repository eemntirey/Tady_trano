CREATE USER IF NOT EXISTS root@localhost IDENTIFIED BY 'rootpass';
GRANT ALL PRIVILEGES ON *.* TO root@localhost WITH GRANT OPTION;
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS tady_trano CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tady_trano;
SOURCE c:/Users/eemntirey/eclipse-workspace/Tady_trano/tady-trano-api/src/main/resources/db/migration/V1__init.sql;
SOURCE c:/Users/eemntirey/eclipse-workspace/Tady_trano/tady-trano-api/src/main/resources/db/migration/V2__donnees_initiales.sql;