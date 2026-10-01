-- ============================================================
-- TaskFlow Database Schema & Seed Data
-- Engine: MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS taskflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE taskflow;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS activities;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS task_labels;
DROP TABLE IF EXISTS labels;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS columns;
DROP TABLE IF EXISTS board_members;
DROP TABLE IF EXISTS boards;
DROP TABLE IF EXISTS workspace_members;
DROP TABLE IF EXISTS workspaces;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Roles Table
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO roles (id, name) VALUES (1, 'ADMIN'), (2, 'MANAGER'), (3, 'MEMBER'), (4, 'VIEWER');

-- 2. Users Table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    system_role_id INT NOT NULL DEFAULT 3,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (system_role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

-- 3. Workspaces Table
CREATE TABLE workspaces (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    owner_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Workspace Members Table
CREATE TABLE workspace_members (
    id INT PRIMARY KEY AUTO_INCREMENT,
    workspace_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ws_user (workspace_id, user_id),
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Boards Table
CREATE TABLE boards (
    id INT PRIMARY KEY AUTO_INCREMENT,
    workspace_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Board Members Table
CREATE TABLE board_members (
    id INT PRIMARY KEY AUTO_INCREMENT,
    board_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_board_user (board_id, user_id),
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Columns Table
CREATE TABLE columns (
    id INT PRIMARY KEY AUTO_INCREMENT,
    board_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    INDEX idx_col_board_pos (board_id, position)
) ENGINE=InnoDB;

-- 8. Tasks Table
CREATE TABLE tasks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    quick_note TEXT NULL,
    board_id INT NOT NULL,
    column_id INT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    assignee_id INT NULL,
    creator_id INT NOT NULL,
    due_date DATETIME NULL,
    estimated_effort_hours DECIMAL(5,2) DEFAULT 0.0,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    FOREIGN KEY (column_id) REFERENCES columns(id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_task_board (board_id),
    INDEX idx_task_column (column_id),
    INDEX idx_task_assignee (assignee_id),
    INDEX idx_task_due (due_date)
) ENGINE=InnoDB;

-- 9. Labels Table
CREATE TABLE labels (
    id INT PRIMARY KEY AUTO_INCREMENT,
    board_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color_hex VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    UNIQUE KEY uk_board_label (board_id, name)
) ENGINE=InnoDB;

-- 10. Task Labels Junction Table
CREATE TABLE task_labels (
    task_id INT NOT NULL,
    label_id INT NOT NULL,
    PRIMARY KEY (task_id, label_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. Comments Table
CREATE TABLE comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    task_id INT NOT NULL,
    user_id INT NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 12. Activity Logs Table
CREATE TABLE activities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    board_id INT NOT NULL,
    task_id INT NULL,
    user_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_act_board (board_id)
) ENGINE=InnoDB;

-- 13. Notifications Table
CREATE TABLE notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    recipient_id INT NOT NULL,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'TASK_ASSIGNED',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (recipient_id, is_read)
) ENGINE=InnoDB;

-- ============================================================
-- DEMO SEED DATA (Password for all demo accounts: Password@123)
-- SHA-256 Hash: 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
-- ============================================================

INSERT INTO users (id, name, email, password_hash, system_role_id) VALUES
(1, 'System Admin', 'admin@taskflow.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1),
(2, 'Project Manager', 'manager@taskflow.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2),
(3, 'Rahul Sharma', 'member@taskflow.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 3),
(4, 'Priya Verma', 'viewer@taskflow.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 4);

INSERT INTO workspaces (id, name, owner_id) VALUES (1, 'Main College Workspace', 1);

INSERT INTO workspace_members (workspace_id, user_id, role) VALUES
(1, 1, 'ADMIN'), (1, 2, 'MANAGER'), (1, 3, 'MEMBER'), (1, 4, 'VIEWER');

INSERT INTO boards (id, workspace_id, name, description, owner_id) VALUES
(1, 1, 'TaskFlow Web Development', 'Main project board for Java Full Stack application.', 1);

INSERT INTO board_members (board_id, user_id, role) VALUES
(1, 1, 'ADMIN'), (1, 2, 'MANAGER'), (1, 3, 'MEMBER'), (1, 4, 'VIEWER');

INSERT INTO columns (id, board_id, name, position) VALUES
(1, 1, 'BACKLOG', 0),
(2, 1, 'TODO', 1),
(3, 1, 'IN PROGRESS', 2),
(4, 1, 'REVIEW', 3),
(5, 1, 'DONE', 4);

INSERT INTO labels (id, board_id, name, color_hex) VALUES
(1, 1, 'BUG', '#EF4444'),
(2, 1, 'FEATURE', '#3B82F6'),
(3, 1, 'BACKEND', '#8B5CF6'),
(4, 1, 'FRONTEND', '#10B981'),
(5, 1, 'DATABASE', '#F59E0B');

INSERT INTO tasks (id, title, description, board_id, column_id, priority, assignee_id, creator_id, due_date, estimated_effort_hours, position) VALUES
(1, 'Set up JDBC Connection Pool', 'Configure MySQL DataSource and DAO layer.', 1, 5, 'HIGH', 3, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 4.0, 0),
(2, 'Implement Backend Role Validation', 'Enforce ADMIN/MANAGER/MEMBER checks in Servlets.', 1, 3, 'URGENT', 3, 2, DATE_ADD(NOW(), INTERVAL 2 DAY), 6.0, 0),
(3, 'Design Responsive Kanban Board UI', 'Build React drag and drop column components.', 1, 3, 'MEDIUM', 2, 1, DATE_ADD(NOW(), INTERVAL 3 DAY), 8.0, 1),
(4, 'Write SQL Subqueries for Dashboard', 'Retrieve member workload stats using aggregate subqueries.', 1, 2, 'LOW', 3, 2, DATE_ADD(NOW(), INTERVAL 5 DAY), 3.0, 0),
(5, 'Prepare System Status JSP Page', 'Demonstrate Servlet to JSP MVC flow for syllabus compliance.', 1, 1, 'LOW', NULL, 1, DATE_ADD(NOW(), INTERVAL 7 DAY), 2.0, 0);

INSERT INTO task_labels (task_id, label_id) VALUES
(1, 3), (1, 5), (2, 3), (3, 4), (4, 5), (5, 3);

INSERT INTO comments (id, task_id, user_id, comment_text) VALUES
(1, 2, 2, 'Please ensure DatabaseException is thrown if the user lacks authority.'),
(2, 2, 3, 'Working on it. Will finish by tomorrow afternoon.');

INSERT INTO activities (board_id, task_id, user_id, action_type, description) VALUES
(1, 2, 2, 'TASK_ASSIGNED', 'Manager assigned Implement Backend Role Validation to Rahul Sharma.'),
(1, 3, 1, 'TASK_MOVED', 'System Admin moved Design Responsive Kanban Board UI to IN PROGRESS.');