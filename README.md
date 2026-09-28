# ⚡ TaskFlow — Multi-User Kanban System

> **Java Full Stack Project** | Servlet API • JDBC • MySQL • React 18 • Multithreading

TaskFlow is a modern, web-based Kanban board built for team collaboration. It enforces **strict backend role-based access control**, Persists normalized relational data, and features background automated deadline reminders.

---

## 🎯 At a Glance

| Layer | Technology Stack |
| :--- | :--- |
| **Frontend** | React 18, Vite, React Router v6, Axios, Lucide Icons |
| **Backend API** | Java 17, Jakarta Servlets 6.0, JDBC, Jackson JSON |
| **Database** | MySQL 8.0+ (`ENGINE=InnoDB`, FK Constraints, Indexes) |
| **Server View (MVC)** | JSP 3.1 (Servlet-forwarded admin health view) |
| **Concurrency** | `java.util.concurrent.ScheduledExecutorService` |
| **Build System** | Apache Maven 3.8+ |

---

## 🔑 Demo Login Accounts

> 💡 **Default Password for all accounts:** `Password@123`

 Email                   Role     Permissions  Summary                          

 admin@taskflow.local    ADMIN    Manage users, roles, system health & tasks   
 manager@taskflow.local  MANAGER  Create/edit/delete tasks, assign deadlines    member@taskflow.local   MEMBER   Update assigned task status, move cards      
 viewer@taskflow.local   VIEWER   Read-only view (cannot create or edit tasks)