package com.clouddrive.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_items", indexes = {
    @Index(name = "idx_owner_path", columnList = "owner_id, path, is_trashed"),
    @Index(name = "idx_owner_starred", columnList = "owner_id, is_starred")
})
public class FileItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1000)
    private String path; // Virtual directory path, e.g. "/" or "/Documents"

    @Column(nullable = false)
    private boolean isDirectory;

    @Column(nullable = false)
    private Long size = 0L;

    @Column(length = 100)
    private String contentType;

    @Column(length = 50)
    private String extension;

    @Column(nullable = false)
    private boolean isStarred = false;

    @Column(nullable = false)
    private boolean isTrashed = false;

    private LocalDateTime trashedAt;

    @Column(length = 1000)
    private String originalPath;

    @Column(nullable = false, length = 500)
    private String diskPath; // Physical disk path inside storage_root

    @Column(length = 20)
    private String color; // e.g., "#3b82f6", "#ef4444"

    @Column(length = 255)
    private String tags; // e.g. "Work,Personal"

    @Column(nullable = false, columnDefinition = "integer default 1")
    private int versionCount = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public FileItem() {}

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

    public String getOriginalPath() { return originalPath; }
    public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }

    public String getDiskPath() { return diskPath; }
    public void setDiskPath(String diskPath) { this.diskPath = diskPath; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public int getVersionCount() { return versionCount; }
    public void setVersionCount(int versionCount) { this.versionCount = versionCount; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
