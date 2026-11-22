package com.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;

public class MoveRequest {
    @NotBlank(message = "Target path is required")
    private String targetPath;

    public MoveRequest() {}

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
}
