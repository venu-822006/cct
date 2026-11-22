package com.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;

public class RenameRequest {
    @NotBlank(message = "New name is required")
    private String newName;

    public RenameRequest() {}

    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }
}
