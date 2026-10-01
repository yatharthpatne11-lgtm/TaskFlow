package com.taskflow.controller;

import com.taskflow.config.DbConfig;
import com.taskflow.dao.TaskDAO;
import com.taskflow.model.User;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;
import java.sql.Connection;

@WebServlet("/admin/status")
public class AdminStatusServlet extends HttpServlet {

    private final TaskDAO taskDAO = new TaskDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("user") : null;

        String dbStatus = "CONNECTED";
        try (Connection conn = DbConfig.getConnection()) {
            if (conn.isClosed()) dbStatus = "DISCONNECTED";
        } catch (Exception e) {
            dbStatus = "ERROR: " + e.getMessage();
        }

        int activeTasks = taskDAO.countActiveTasks();

        req.setAttribute("currentUser", currentUser);
        req.setAttribute("dbStatus", dbStatus);
        req.setAttribute("activeTaskCount", activeTasks);

        req.getRequestDispatcher("/WEB-INF/jsp/admin-status.jsp").forward(req, resp);
    }
}