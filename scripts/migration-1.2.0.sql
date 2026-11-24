-- Database migration from spring-auth 1.1.x to 1.2.0 (MariaDB)
--
-- Run it once on the production database BEFORE starting version 1.2.0:
--   mariadb -h <host> -u <user> -p <database> < scripts/migration-1.2.0.sql
--
-- Production validates the schema at startup (spring.jpa.hibernate.ddl-auto=validate by default),
-- so the application does not start while the new tables are missing. Even with
-- SPRING_DDL_AUTO=update, Hibernate never changes existing columns, so the users.last_name change
-- below is needed in both cases.
--
-- The script can safely be run more than once.

-- Refresh tokens are now stored (hashed) to allow rotation and revocation
CREATE TABLE IF NOT EXISTS refresh_tokens (
    revoked bit NOT NULL,
    created_at datetime(6),
    expires_at datetime(6) NOT NULL,
    id bigint NOT NULL AUTO_INCREMENT,
    token_hash varchar(255) NOT NULL,
    user_login varchar(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKo2mlirhldriil2y7krapq4frt UNIQUE (token_hash)
) ENGINE=InnoDB;

-- One-time authentication codes, exchanged for tokens after an Azure login (POST /oauth2/token)
CREATE TABLE IF NOT EXISTS auth_codes (
    created_at datetime(6) NOT NULL,
    expires_at datetime(6) NOT NULL,
    id bigint NOT NULL AUTO_INCREMENT,
    code varchar(255) NOT NULL,
    redirect_url varchar(255) NOT NULL,
    user_login varchar(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKhwc6mero8h6vx8g2fvjeje9ud UNIQUE (code)
) ENGINE=InnoDB;

-- Azure users without a last name can now register
ALTER TABLE users MODIFY last_name varchar(255) NULL;
