package com.taskflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskflow.dao.UserDAO;
import com.taskflow.dto.ApiResponse;
import com.taskflow.model.User;
import com.taskflow.util.PasswordUtil;
import com.taskflow.util.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Admin-only user management (role & authorization control).
 *   GET  /api/users          -> list all users (never includes password hashes)
 *   POST /api/users          -> create a user   { name, email, password, roleId }
 *   PUT  /api/users/role     -> change role     { userId, roleId }      (1 ADMIN, 2 MANAGER, 3 MEMBER, 4 VIEWER)
 *   PUT  /api/users/status   -> change status   { userId, status }      (ACTIVE, INACTIVE, SUSPENDED)
 */
@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {

    private static final Set<String> STATUSES = new HashSet<>(Arrays.asList("ACTIVE", "INACTIVE", "SUSPENDED"));

    private final UserDAO userDAO = new UserDAO();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ---------- helpers ----------

    private void json(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
    }

    private void fail(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        objectMapper.writeValue(resp.getWriter(), ApiResponse.error(message));
    }

    /** Returns the logged-in ADMIN (role re-checked in the database), or writes 401/403 and returns null. */
    private User requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = SessionUser.current(req);
        if (current == null) {
            fail(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return null;
        }
        if (!"ADMIN".equalsIgnoreCase(current.getSystemRoleName())) {
            fail(resp, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Only ADMIN can manage users.");
            return null;
        }
        return current;
    }

    /** Copy of the user without the password hash. */
    private User toSafe(User u) {
        User copy = new User(u.getId(), u.getName(), u.getEmail(), u.getSystemRoleName());
        copy.setSystemRoleId(u.getSystemRoleId());
        copy.setStatus(u.getStatus());
        copy.setCreatedAt(u.getCreatedAt());
        copy.setUpdatedAt(u.getUpdatedAt());
        return copy;
    }

    private static String text(JsonNode body, String field) {
        return (body != null && body.hasNonNull(field)) ? body.get(field).asText().trim() : null;
    }

    // ---------- GET /api/users ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        json(resp);
        if (requireAdmin(req, resp) == null) return;

        try {
            List<User> safe = new ArrayList<>();
            for (User u : userDAO.findAll()) safe.add(toSafe(u));
            objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Users fetched successfully", safe));
        } catch (Exception e) {
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load users: " + e.getMessage());
        }
    }

    // ---------- POST /api/users ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        json(resp);
        if (requireAdmin(req, resp) == null) return;

        String path = req.getPathInfo();
        if (path != null && !path.equals("/")) {
            fail(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            return;
        }

        try {
            JsonNode body = objectMapper.readTree(req.getInputStream());
            String name = text(body, "name");
            String email = text(body, "email");
            String password = text(body, "password");
            int roleId = (body != null && body.hasNonNull("roleId")) ? body.get("roleId").asInt() : 3;

            if (name == null || name.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email and password are required.");
                return;
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid email address.");
                return;
            }
            if (password.length() < 6) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters.");
                return;
            }
            if (roleId < 1 || roleId > 4) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid role.");
                return;
            }
            if (userDAO.findByEmail(email).isPresent()) {
                fail(resp, HttpServletResponse.SC_CONFLICT, "Email is already registered.");
                return;
            }

            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPasswordHash(PasswordUtil.hashPassword(password));
            newUser.setSystemRoleId(roleId);
            newUser.setStatus("ACTIVE");
            User created = userDAO.create(newUser);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.success("User created successfully", toSafe(created)));
        } catch (Exception e) {
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create user: " + e.getMessage());
        }
    }

    // ---------- PUT /api/users/role  and  PUT /api/users/status ----------

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        json(resp);
        User admin = requireAdmin(req, resp);
        if (admin == null) return;

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        boolean isRole = path.matches("^/role/?$");
        boolean isStatus = path.matches("^/status/?$");
        if (!isRole && !isStatus) {
            fail(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            return;
        }

        try {
            JsonNode body = objectMapper.readTree(req.getInputStream());
            if (body == null || !body.hasNonNull("userId")) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, "userId is required.");
                return;
            }
            int userId = body.get("userId").asInt();

            // Safety: admins cannot change their own role or status (prevents locking everyone out)
            if (admin.getId() != null && admin.getId() == userId) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, "You cannot change your own role or status.");
                return;
            }

            Optional<User> target = userDAO.findById(userId);
            if (!target.isPresent()) {
                fail(resp, HttpServletResponse.SC_NOT_FOUND, "User not found.");
                return;
            }

            if (isRole) {
                if (!body.hasNonNull("roleId")) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, "roleId is required.");
                    return;
                }
                int roleId = body.get("roleId").asInt();
                if (roleId < 1 || roleId > 4) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid role.");
                    return;
                }
                userDAO.updateRole(userId, roleId);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Role updated.", null));
            } else {
                String status = text(body, "status");
                if (status == null || !STATUSES.contains(status.toUpperCase())) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, "Status must be ACTIVE, INACTIVE or SUSPENDED.");
                    return;
                }
                userDAO.updateStatus(userId, status.toUpperCase());
                objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Status updated.", null));
            }
        } catch (Exception e) {
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not update user: " + e.getMessage());
        }
    }
}