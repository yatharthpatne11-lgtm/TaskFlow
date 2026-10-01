package com.taskflow.dao;

import com.taskflow.config.DbConfig;
import com.taskflow.exception.DatabaseException;
import com.taskflow.model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskDAO {

    public List<Task> findByBoardId(int boardId) {
        String sql = """
            SELECT t.*, u.name AS assignee_name, c.name AS creator_name, col.name AS column_name
            FROM tasks t
            LEFT JOIN users u ON t.assignee_id = u.id
            JOIN users c ON t.creator_id = c.id
            JOIN columns col ON t.column_id = col.id
            WHERE t.board_id = ?
            ORDER BY col.position ASC, t.position ASC
        """;

        List<Task> tasks = new ArrayList<>();
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, boardId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving tasks for board ID: " + boardId, e);
        }
        return tasks;
    }

    public Task create(Task task) {
        String sql = "INSERT INTO tasks (title, description, board_id, column_id, priority, assignee_id, creator_id, due_date, estimated_effort_hours, position) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setInt(3, task.getBoardId());
            stmt.setInt(4, task.getColumnId());
            stmt.setString(5, task.getPriority());
            if (task.getAssigneeId() != null) stmt.setInt(6, task.getAssigneeId()); else stmt.setNull(6, Types.INTEGER);
            stmt.setInt(7, task.getCreatorId());
            if (task.getDueDate() != null) stmt.setTimestamp(8, Timestamp.valueOf(task.getDueDate())); else stmt.setNull(8, Types.TIMESTAMP);
            stmt.setDouble(9, task.getEstimatedEffortHours() != null ? task.getEstimatedEffortHours() : 0.0);
            stmt.setInt(10, task.getPosition() != null ? task.getPosition() : 0);

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("Creating task failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    task.setId(generatedKeys.getInt(1));
                }
            }
            return task;
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting new task", e);
        }
    }

    public boolean updateColumnAndPosition(int taskId, int columnId, int position) {
        String sql = "UPDATE tasks SET column_id = ?, position = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, columnId);
            stmt.setInt(2, position);
            stmt.setInt(3, taskId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update task column/position", e);
        }
    }

    public boolean updateQuickNote(int taskId, String quickNote) {
        String sql = "UPDATE tasks SET quick_note = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (quickNote != null) stmt.setString(1, quickNote); else stmt.setNull(1, Types.VARCHAR);
            stmt.setInt(2, taskId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update quick note for task ID: " + taskId, e);
        }
    }

    public Optional<Task> findById(int id) {
        String sql = "SELECT t.*, u.name AS assignee_name, c.name AS creator_name, col.name AS column_name FROM tasks t LEFT JOIN users u ON t.assignee_id = u.id JOIN users c ON t.creator_id = c.id JOIN columns col ON t.column_id = col.id WHERE t.id = ?";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapResultSetToTask(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Error finding task by ID", e);
        }
        return Optional.empty();
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting task", e);
        }
    }

    public int countActiveTasks() {
        String sql = "SELECT COUNT(*) FROM tasks";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Error counting active tasks", e);
        }
        return 0;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }

    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        // Older databases may not have the quick_note column yet (see database/migration_add_quick_note.sql).
        // Don't let that take the whole board down - just treat the note as empty.
        t.setQuickNote(hasColumn(rs, "quick_note") ? rs.getString("quick_note") : null);
        t.setBoardId(rs.getInt("board_id"));
        t.setColumnId(rs.getInt("column_id"));
        t.setPriority(rs.getString("priority"));
        int assignee = rs.getInt("assignee_id");
        t.setAssigneeId(rs.wasNull() ? null : assignee);
        t.setAssigneeName(rs.getString("assignee_name"));
        t.setCreatorId(rs.getInt("creator_id"));
        t.setCreatorName(rs.getString("creator_name"));
        t.setColumnName(rs.getString("column_name"));
        Timestamp due = rs.getTimestamp("due_date");
        if (due != null) t.setDueDate(due.toLocalDateTime());
        t.setEstimatedEffortHours(rs.getDouble("estimated_effort_hours"));
        t.setPosition(rs.getInt("position"));
        return t;
    }
}