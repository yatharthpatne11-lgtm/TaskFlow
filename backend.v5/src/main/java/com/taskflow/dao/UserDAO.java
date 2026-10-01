package com.taskflow.dao;

import com.taskflow.exception.DatabaseException;
import com.taskflow.model.User;
import com.taskflow.config.DbConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByEmail(String email) {
    String sql = """
        SELECT u.*, r.name AS role_name 
        FROM users u 
        LEFT JOIN roles r ON u.system_role_id = r.id 
        WHERE u.email = ?
    """;
    try (Connection conn = DbConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, email.trim());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return Optional.of(mapResultSetToUser(rs));
        }
    } catch (SQLException e) {
        throw new DatabaseException("Error finding user by email: " + email, e);
    }
    return Optional.empty();
}

    public Optional<User> findById(int id) {
        String sql = """
            SELECT u.*, r.name AS role_name 
            FROM users u 
            JOIN roles r ON u.system_role_id = r.id 
            WHERE u.id = ?
        """;
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by ID: " + id, e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        String sql = """
            SELECT u.*, r.name AS role_name 
            FROM users u 
            JOIN roles r ON u.system_role_id = r.id 
            ORDER BY u.id ASC
        """;
        List<User> users = new ArrayList<>();
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all users", e);
        }
        return users;
    }

    public User create(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, system_role_id, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setInt(4, user.getSystemRoleId() != null ? user.getSystemRoleId() : 3); // Default MEMBER
            stmt.setString(5, user.getStatus() != null ? user.getStatus() : "ACTIVE");

            int affected = stmt.executeUpdate();
            if (affected == 0) throw new DatabaseException("User creation failed, no rows affected.");

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating user", e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSystemRoleId(rs.getInt("system_role_id"));
        u.setSystemRoleName(rs.getString("role_name"));
        u.setStatus(rs.getString("status"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) u.setCreatedAt(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) u.setUpdatedAt(ut.toLocalDateTime());
        return u;
    }
}