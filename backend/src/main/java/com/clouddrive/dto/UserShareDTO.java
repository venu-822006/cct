package com.clouddrive.dto;

import com.clouddrive.entity.UserShare;
import java.time.LocalDateTime;

public class UserShareDTO {
    private Long id;
    private FileItemDTO fileItem;
    private String sharedByUsername;
    private String sharedByFullName;
    private String sharedWithUsername;
    private String permission; // "VIEW", "EDIT"
    private LocalDateTime createdAt;

    public UserShareDTO() {}

    public static UserShareDTO fromEntity(UserShare s) {
        UserShareDTO dto = new UserShareDTO();
        dto.setId(s.getId());
        dto.setFileItem(FileItemDTO.fromEntity(s.getFileItem()));
        dto.setSharedByUsername(s.getSharedBy().getUsername());
        dto.setSharedByFullName(s.getSharedBy().getFullName());
        dto.setSharedWithUsername(s.getSharedWith().getUsername());
        dto.setPermission(s.getPermission());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FileItemDTO getFileItem() { return fileItem; }
    public void setFileItem(FileItemDTO fileItem) { this.fileItem = fileItem; }

    public String getSharedByUsername() { return sharedByUsername; }
    public void setSharedByUsername(String sharedByUsername) { this.sharedByUsername = sharedByUsername; }

    public String getSharedByFullName() { return sharedByFullName; }
    public void setSharedByFullName(String sharedByFullName) { this.sharedByFullName = sharedByFullName; }

    public String getSharedWithUsername() { return sharedWithUsername; }
    public void setSharedWithUsername(String sharedWithUsername) { this.sharedWithUsername = sharedWithUsername; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
