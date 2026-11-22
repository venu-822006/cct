package com.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateFolderRequest {
    @NotBlank(message = "Folder name is required")
    private String name;

    private String path = "/";

    public CreateFolderRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
