package com.clouddrive.dto;

import com.clouddrive.entity.FileVersion;
import java.time.LocalDateTime;

public class FileVersionDTO {
    private Long id;
    private int versionNumber;
    private Long size;
    private String changeSummary;
    private LocalDateTime createdAt;

    public FileVersionDTO() {}

    public static FileVersionDTO fromEntity(FileVersion v) {
        FileVersionDTO dto = new FileVersionDTO();
        dto.setId(v.getId());
        dto.setVersionNumber(v.getVersionNumber());
        dto.setSize(v.getSize());
        dto.setChangeSummary(v.getChangeSummary());
        dto.setCreatedAt(v.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
