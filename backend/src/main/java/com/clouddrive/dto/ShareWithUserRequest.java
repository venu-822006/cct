package com.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ShareWithUserRequest {
    @NotNull(message = "File ID is required")
    private Long fileId;

    @NotBlank(message = "Target username is required")
    private String targetUsername;

    private String permission = "VIEW"; // "VIEW" or "EDIT"

    public ShareWithUserRequest() {}

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
