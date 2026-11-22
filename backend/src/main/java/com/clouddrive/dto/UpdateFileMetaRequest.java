package com.clouddrive.dto;

public class UpdateFileMetaRequest {
    private String color;
    private String tags;

    public UpdateFileMetaRequest() {}

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
