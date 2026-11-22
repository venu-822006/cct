package com.clouddrive.dto;

import java.util.List;

public class BatchActionRequest {
    private List<Long> ids;
    private String action; // "TRASH", "STAR", "UNSTAR", "RESTORE", "MOVE"
    private String targetPath;

    public BatchActionRequest() {}

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
}
