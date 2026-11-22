package com.clouddrive.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_shares", indexes = {
    @Index(name = "idx_shared_with", columnList = "shared_with_id")
})
public class UserShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_id", nullable = false)
    private FileItem fileItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    @Column(nullable = false, length = 20)
    private String permission = "VIEW"; // "VIEW" or "EDIT"

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserShare() {}

    public UserShare(FileItem fileItem, User sharedBy, User sharedWith, String permission) {
        this.fileItem = fileItem;
        this.sharedBy = sharedBy;
        this.sharedWith = sharedWith;
        this.permission = permission;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FileItem getFileItem() { return fileItem; }
    public void setFileItem(FileItem fileItem) { this.fileItem = fileItem; }

    public User getSharedBy() { return sharedBy; }
    public void setSharedBy(User sharedBy) { this.sharedBy = sharedBy; }

    public User getSharedWith() { return sharedWith; }
    public void setSharedWith(User sharedWith) { this.sharedWith = sharedWith; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
