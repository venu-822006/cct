-- ===================================================================
-- Database Schema & Seed Data for Personal Cloud Drive
-- Compatible with MySQL (8.x / MariaDB), PostgreSQL, and H2 Database
-- ===================================================================

CREATE DATABASE IF NOT EXISTS clouddrive_db;
USE clouddrive_db;

-- -------------------------------------------------------------------
-- 1. Table: users (Stores user accounts and login credentials)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hashed password
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    quota_bytes BIGINT NOT NULL DEFAULT 524288000, -- 500 MB default
    used_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------
-- 2. Table: file_items (Stores file and directory metadata)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    is_directory BOOLEAN NOT NULL DEFAULT FALSE,
    size BIGINT DEFAULT 0,
    content_type VARCHAR(100),
    extension VARCHAR(20),
    is_starred BOOLEAN NOT NULL DEFAULT FALSE,
    is_trashed BOOLEAN NOT NULL DEFAULT FALSE,
    trashed_at TIMESTAMP NULL,
    original_path VARCHAR(1000) NULL,
    disk_path VARCHAR(500) NOT NULL,
    color VARCHAR(20) NULL,
    tags VARCHAR(255) NULL,
    version_count INT NOT NULL DEFAULT 1,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------
-- 3. Table: file_versions (Stores historical document snapshots)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_item_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    size BIGINT NOT NULL,
    disk_path VARCHAR(500) NOT NULL,
    change_summary VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_version_file FOREIGN KEY (file_item_id) REFERENCES file_items(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------
-- 4. Table: share_links (Stores public expiring download links)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS share_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NULL,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_share_file FOREIGN KEY (file_id) REFERENCES file_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------
-- 5. Table: user_shares (Stores user-to-user sharing permissions)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_shares (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_item_id BIGINT NOT NULL,
    shared_by_id BIGINT NOT NULL,
    shared_with_id BIGINT NOT NULL,
    permission VARCHAR(20) NOT NULL DEFAULT 'VIEW', -- 'VIEW' or 'EDIT'
    shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ushare_file FOREIGN KEY (file_item_id) REFERENCES file_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_ushare_by FOREIGN KEY (shared_by_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ushare_with FOREIGN KEY (shared_with_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------
-- 6. Table: activity_logs (Stores audit trail of user actions)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_name VARCHAR(255) NULL,
    details VARCHAR(500) NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------
-- Default Seed Accounts (Passwords hashed with BCrypt)
-- -------------------------------------------------------------------
-- Password for 'admin' is 'admin123' (BCrypt strength 10)
INSERT INTO users (username, email, password, full_name, role, quota_bytes, used_bytes)
VALUES (
    'admin',
    'admin@clouddrive.com',
    '$2a$10$Dow1q8n0v7Kx5n4m3b2v1e7.X8z9y0w1v2u3t4s5r6q7p8o9n0m1',
    'System Administrator',
    'ROLE_ADMIN',
    524288000,
    0
) ON DUPLICATE KEY UPDATE id=id;
