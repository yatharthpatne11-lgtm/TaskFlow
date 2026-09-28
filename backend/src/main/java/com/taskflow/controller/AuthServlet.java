package com.taskflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskflow.dao.UserDAO;
import com.taskflow.dto.ApiResponse;
import com.taskflow.model.User;
import com.taskflow.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ---------- GET /api/auth/me ----------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || !pathInfo.equalsIgnoreCase("/me")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Endpoint not found"));
            return;
        }

        HttpSession session = request.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (sessionUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Not authenticated"));
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), ApiResponse.success("Active session", sanitize(sessionUser)));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Invalid endpoint requested"));
            return;
        }

        JsonNode body;
        try {
            body = objectMapper.readTree(request.getInputStream());
            if (body == null) {
                body = objectMapper.createObjectNode();
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Malformed JSON body"));
            return;
        }

        switch (pathInfo.toLowerCase()) {
            case "/login":
                handleLogin(body, request, response);
                break;

            case "/register":
                handleRegister(body, response);
                break;

            case "/logout":
                handleLogout(request, response);
                break;

            default:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(response.getWriter(), ApiResponse.error("Endpoint not found"));
                break;
        }
    }

    /**
     * Handles User Login
     */
    private void handleLogin(JsonNode body, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String email = textOrNull(body, "email");
        String plainPassword = textOrNull(body, "password");

        if (email == null || plainPassword == null || email.trim().isEmpty() || plainPassword.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Email and password are required"));
            return;
        }

        try {
            User user = userDAO.findByEmail(email.trim()).orElse(null);

            if (user != null && PasswordUtil.verifyPassword(plainPassword.trim(), user.getPasswordHash())) {

                HttpSession session = request.getSession(true);
                session.setAttribute("user", user);

                response.setStatus(HttpServletResponse.SC_OK);
                objectMapper.writeValue(response.getWriter(), ApiResponse.success("Login successful", sanitize(user)));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                objectMapper.writeValue(response.getWriter(), ApiResponse.error("Invalid email or password"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("An internal server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Handles User Registration
     */
    private void handleRegister(JsonNode body, HttpServletResponse response) throws IOException {

        String name = textOrNull(body, "name");
        String email = textOrNull(body, "email");
        String plainPassword = textOrNull(body, "password");

        if (name == null || email == null || plainPassword == null ||
            name.trim().isEmpty() || email.trim().isEmpty() || plainPassword.trim().isEmpty()) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Name, email, and password are required"));
            return;
        }

        try {
            if (userDAO.findByEmail(email.trim()).isPresent()) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                objectMapper.writeValue(response.getWriter(), ApiResponse.error("Email is already registered"));
                return;
            }

            String hashedPassword = PasswordUtil.hashPassword(plainPassword.trim());

            User newUser = new User();
            newUser.setName(name.trim());
            newUser.setEmail(email.trim());
            newUser.setPasswordHash(hashedPassword);
            newUser.setSystemRoleId(3); // Default MEMBER role
            newUser.setStatus("ACTIVE");

            User createdUser = userDAO.create(newUser);

            response.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.success("User registered successfully", sanitize(createdUser)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("An internal server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Handles User Logout
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), ApiResponse.success("Logout successful", null));
    }

    private String textOrNull(JsonNode body, String field) {
        JsonNode node = body.get(field);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    /** Never send the password hash back to the client. */
    private User sanitize(User user) {
        User copy = new User(user.getId(), user.getName(), user.getEmail(), user.getSystemRoleName());
        copy.setSystemRoleId(user.getSystemRoleId());
        copy.setStatus(user.getStatus());
        copy.setCreatedAt(user.getCreatedAt());
        copy.setUpdatedAt(user.getUpdatedAt());
        return copy;
    }
}
