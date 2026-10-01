package com.taskflow.model;

import java.time.LocalDateTime;

public class Task {
    private Integer id;
    private String title;
    private String description;
    private String quickNote;
    private Integer boardId;
    private Integer columnId;
    private String columnName;
    private String priority;
    private Integer assigneeId;
    private String assigneeName;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime dueDate;
    private Double estimatedEffortHours;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQuickNote() { return quickNote; }
    public void setQuickNote(String quickNote) { this.quickNote = quickNote; }

    public Integer getBoardId() { return boardId; }
    public void setBoardId(Integer boardId) { this.boardId = boardId; }

    public Integer getColumnId() { return columnId; }
    public void setColumnId(Integer columnId) { this.columnId = columnId; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public Integer getCreatorId() { return creatorId; }
    public void setCreatorId(Integer creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public Double getEstimatedEffortHours() { return estimatedEffortHours; }
    public void setEstimatedEffortHours(Double estimatedEffortHours) { this.estimatedEffortHours = estimatedEffortHours; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}