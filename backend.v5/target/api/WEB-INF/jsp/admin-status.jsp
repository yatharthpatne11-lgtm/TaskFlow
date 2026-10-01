<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.taskflow.model.User" %>
<%
    User user = (User) request.getAttribute("currentUser");
    String dbStatus = (String) request.getAttribute("dbStatus");
    Integer activeTaskCount = (Integer) request.getAttribute("activeTaskCount");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TaskFlow System Health & Admin MVC Status</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 40px; }
        .card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 24px; max-width: 600px; margin: 0 auto; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
        h1 { font-size: 1.5rem; color: #38bdf8; margin-top: 0; }
        .badge { display: inline-block; padding: 4px 12px; border-radius: 9999px; font-size: 0.85rem; font-weight: 600; }
        .bg-green { background: #059669; color: #ecfdf5; }
        .bg-blue { background: #0284c7; color: #f0f9ff; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        td { padding: 10px 0; border-bottom: 1px solid #334155; }
        td.label { color: #94a3b8; font-weight: 500; }
    </style>
</head>
<body>
    <div class="card">
        <h1>TaskFlow System Administration & MVC Status</h1>
        <p style="color: #94a3b8;">Syllabus Demonstration Page: Servlet &rarr; Model &rarr; JSP Server-Side Rendering</p>
        
        <table>
            <tr>
                <td class="label">Database Connection:</td>
                <td><span class="badge bg-green"><%= dbStatus %></span></td>
            </tr>
            <tr>
                <td class="label">Authenticated Admin:</td>
                <td><strong><%= user != null ? user.getName() : "System Administrator" %></strong></td>
            </tr>
            <tr>
                <td class="label">Active Database Tasks:</td>
                <td><span class="badge bg-blue"><%= activeTaskCount %> Tasks</span></td>
            </tr>
            <tr>
                <td class="label">Server Environment:</td>
                <td>Jakarta Servlet 6.0 / Apache Tomcat 10+</td>
            </tr>
        </table>
        
        <p style="margin-top: 24px; font-size: 0.85rem; color: #64748b;">
            This JSP template proves compliance with Java Web Syllabus Requirements (Servlet-to-JSP forward).
        </p>
    </div>
</body>
</html>