package com.clouddrive.dto;

public class ShareRequest {
    private Long fileId;
    private Integer expiresInHours; // null for no expiration

    public ShareRequest() {}

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public Integer getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(Integer expiresInHours) { this.expiresInHours = expiresInHours; }
}
