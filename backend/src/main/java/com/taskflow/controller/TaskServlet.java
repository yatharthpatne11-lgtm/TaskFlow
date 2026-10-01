package com.taskflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskflow.dao.TaskDAO;
import com.taskflow.dto.ApiResponse;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/api/tasks/*")
public class TaskServlet extends HttpServlet {

    private static final int MAX_QUICK_NOTE_LENGTH = 1000;
    private static final Pattern QUICK_NOTE_PATH = Pattern.compile("^/(\\d+)/quick-note/?$");

    private final TaskDAO taskDAO = new TaskDAO();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Servlet 3.0 HttpServlet has no doPatch(), so PATCH requests are routed manually.
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String boardIdParam = req.getParameter("boardId");

        try {
            if (boardIdParam != null) {
                int boardId = Integer.parseInt(boardIdParam);
                List<Task> tasks = taskDAO.findByBoardId(boardId);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Tasks fetched successfully", tasks));
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.error("boardId parameter required."));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Authentication required"));
            return;
        }

        if ("VIEWER".equalsIgnoreCase(currentUser.getSystemRoleName())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("VIEWER role is not allowed to create tasks."));
            return;
        }

        try {
            Task task = objectMapper.readValue(req.getInputStream(), Task.class);
            task.setCreatorId(currentUser.getId());
            Task created = taskDAO.create(task);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Task created successfully", created));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (currentUser == null || "VIEWER".equalsIgnoreCase(currentUser.getSystemRoleName())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Insufficient authorization to update task."));
            return;
        }

        try {
            Task taskUpdate = objectMapper.readValue(req.getInputStream(), Task.class);
            boolean updated = taskDAO.updateColumnAndPosition(taskUpdate.getId(), taskUpdate.getColumnId(), taskUpdate.getPosition() != null ? taskUpdate.getPosition() : 0);
            
            if (updated) {
                objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Task moved successfully", taskUpdate));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Task not found"));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error(e.getMessage()));
        }
    }

    // PATCH /api/tasks/{id}/quick-note  -> body: { "quickNote": "text" }
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Authentication required"));
            return;
        }

        String pathInfo = req.getPathInfo();
        Matcher matcher = QUICK_NOTE_PATH.matcher(pathInfo == null ? "" : pathInfo);
        if (!matcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Endpoint not found"));
            return;
        }

        try {
            int taskId = Integer.parseInt(matcher.group(1));
            Optional<Task> found = taskDAO.findById(taskId);

            if (!found.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Task not found"));
                return;
            }

            Task task = found.get();

            // Only the assigned user may edit the quick note (compared against the session user, never the request body)
            if (task.getAssigneeId() == null || !task.getAssigneeId().equals(currentUser.getId())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Only the assigned user can edit this quick note."));
                return;
            }

            JsonNode body = objectMapper.readTree(req.getInputStream());
            if (body == null || !body.has("quickNote")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.error("quickNote field is required."));
                return;
            }

            String quickNote = body.get("quickNote").isNull() ? "" : body.get("quickNote").asText().trim();
            if (quickNote.length() > MAX_QUICK_NOTE_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(resp.getWriter(),
                        ApiResponse.error("Quick note cannot exceed " + MAX_QUICK_NOTE_LENGTH + " characters."));
                return;
            }

            taskDAO.updateQuickNote(taskId, quickNote.isEmpty() ? null : quickNote);
            Task updated = taskDAO.findById(taskId).orElse(task);

            objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Quick note updated successfully", updated));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (currentUser == null || (!"ADMIN".equalsIgnoreCase(currentUser.getSystemRoleName()) && !"MANAGER".equalsIgnoreCase(currentUser.getSystemRoleName()))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Access Denied: Only ADMIN or MANAGER can delete tasks."));
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            int taskId = Integer.parseInt(pathInfo.substring(1));
            boolean deleted = taskDAO.delete(taskId);
            if (deleted) {
                objectMapper.writeValue(resp.getWriter(), ApiResponse.success("Task deleted successfully", null));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Task ID not found"));
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(resp.getWriter(), ApiResponse.error("Missing task ID"));
        }
    }
}