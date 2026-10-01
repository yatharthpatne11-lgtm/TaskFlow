-- Run once on an EXISTING taskflow database (fresh installs get this column from taskflow.sql)
USE taskflow;
ALTER TABLE tasks ADD COLUMN quick_note TEXT NULL AFTER description;
