-- ============================================================
-- TaskFlow: restore the Kanban board WITHOUT dropping any data
-- Safe to run more than once. Does NOT touch the users table.
-- Run:  mysql -u root -p < restore_board.sql
-- ============================================================
USE taskflow;

-- 1) Add tasks.quick_note if the database doesn't have it yet
--    (this missing column is what made every task query fail)
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'quick_note');
SET @ddl := IF(@has_col = 0,
               'ALTER TABLE tasks ADD COLUMN quick_note TEXT NULL AFTER description',
               'SELECT ''quick_note column already exists'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Re-create the workspace / board / columns only if they are missing
INSERT IGNORE INTO workspaces (id, name, owner_id) VALUES (1, 'Main College Workspace', 1);

INSERT IGNORE INTO boards (id, workspace_id, name, description, owner_id)
VALUES (1, 1, 'TaskFlow Web Development', 'Main project board for Java Full Stack application.', 1);

INSERT IGNORE INTO columns (id, board_id, name, position) VALUES
(1, 1, 'BACKLOG', 0), (2, 1, 'TODO', 1), (3, 1, 'IN PROGRESS', 2), (4, 1, 'REVIEW', 3), (5, 1, 'DONE', 4);

-- 3) Re-create the demo tasks only if board 1 has no tasks at all
INSERT INTO tasks (id, title, description, board_id, column_id, priority, assignee_id, creator_id, due_date, estimated_effort_hours, position)
SELECT * FROM (
  SELECT 1 id, 'Set up JDBC Connection Pool' title, 'Configure MySQL DataSource and DAO layer.' description, 1 board_id, 5 column_id, 'HIGH' priority, 3 assignee_id, 1 creator_id, DATE_ADD(NOW(), INTERVAL 1 DAY) due_date, 4.0 eh, 0 pos UNION ALL
  SELECT 2, 'Implement Backend Role Validation', 'Enforce ADMIN/MANAGER/MEMBER checks in Servlets.', 1, 3, 'URGENT', 3, 2, DATE_ADD(NOW(), INTERVAL 2 DAY), 6.0, 0 UNION ALL
  SELECT 3, 'Design Responsive Kanban Board UI', 'Build React drag and drop column components.', 1, 3, 'MEDIUM', 2, 1, DATE_ADD(NOW(), INTERVAL 3 DAY), 8.0, 1 UNION ALL
  SELECT 4, 'Write SQL Subqueries for Dashboard', 'Retrieve member workload stats using aggregate subqueries.', 1, 2, 'LOW', 3, 2, DATE_ADD(NOW(), INTERVAL 5 DAY), 3.0, 0 UNION ALL
  SELECT 5, 'Prepare System Status JSP Page', 'Demonstrate Servlet to JSP MVC flow for syllabus compliance.', 1, 1, 'LOW', NULL, 1, DATE_ADD(NOW(), INTERVAL 7 DAY), 2.0, 0
) seed
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE board_id = 1);

-- 4) Make sure every existing user can see the workspace/board
INSERT IGNORE INTO workspace_members (workspace_id, user_id, role)
  SELECT 1, u.id, 'MEMBER' FROM users u;
INSERT IGNORE INTO board_members (board_id, user_id, role)
  SELECT 1, u.id, 'MEMBER' FROM users u;

-- 5) Sanity check
SELECT 'boards' AS tbl, COUNT(*) AS n FROM boards
UNION ALL SELECT 'columns', COUNT(*) FROM columns
UNION ALL SELECT 'tasks on board 1', COUNT(*) FROM tasks WHERE board_id = 1;