package com.clouddrive.dto;

import com.clouddrive.entity.ActivityLog;
import java.time.LocalDateTime;

public class ActivityLogDTO {
    private Long id;
    private String action;
    private String targetName;
    private String details;
    private LocalDateTime timestamp;

    public ActivityLogDTO() {}

    public static ActivityLogDTO fromEntity(ActivityLog log) {
        ActivityLogDTO dto = new ActivityLogDTO();
        dto.setId(log.getId());
        dto.setAction(log.getAction());
        dto.setTargetName(log.getTargetName());
        dto.setDetails(log.getDetails());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
