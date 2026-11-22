package com.clouddrive.dto;

import com.clouddrive.entity.FileItem;
import java.time.LocalDateTime;

public class FileItemDTO {
    private Long id;
    private String name;
    private String path;
    private boolean isDirectory;
    private Long size;
    private String contentType;
    private String extension;
    private boolean isStarred;
    private boolean isTrashed;
    private LocalDateTime trashedAt;
    private String color;
    private String tags;
    private int versionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FileItemDTO() {}

    public static FileItemDTO fromEntity(FileItem item) {
        FileItemDTO dto = new FileItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setPath(item.getPath());
        dto.setDirectory(item.isDirectory());
        dto.setSize(item.getSize());
        dto.setContentType(item.getContentType());
        dto.setExtension(item.getExtension());
        dto.setStarred(item.isStarred());
        dto.setTrashed(item.isTrashed());
        dto.setTrashedAt(item.getTrashedAt());
        dto.setColor(item.getColor());
        dto.setTags(item.getTags());
        dto.setVersionCount(item.getVersionCount());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    public boolean isTrashed() { return isTrashed; }
    public void setTrashed(boolean trashed) { isTrashed = trashed; }

    public LocalDateTime getTrashedAt() { return trashedAt; }
    public void setTrashedAt(LocalDateTime trashedAt) { this.trashedAt = trashedAt; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public int getVersionCount() { return versionCount; }
    public void setVersionCount(int versionCount) { this.versionCount = versionCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
