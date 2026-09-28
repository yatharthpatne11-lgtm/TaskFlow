package com.taskflow.controller;

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

@WebServlet("/api/tasks/*")
public class TaskServlet extends HttpServlet {

    private final TaskDAO taskDAO = new TaskDAO();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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