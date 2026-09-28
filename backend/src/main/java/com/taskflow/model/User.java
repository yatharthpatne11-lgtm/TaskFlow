package com.taskflow.model;

import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String name;
    private String email;
    private String passwordHash;
    private Integer systemRoleId;
    private String systemRoleName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(Integer id, String name, String email, String systemRoleName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.systemRoleName = systemRoleName;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Integer getSystemRoleId() { return systemRoleId; }
    public void setSystemRoleId(Integer systemRoleId) { this.systemRoleId = systemRoleId; }

    public String getSystemRoleName() { return systemRoleName; }
    public void setSystemRoleName(String systemRoleName) { this.systemRoleName = systemRoleName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}