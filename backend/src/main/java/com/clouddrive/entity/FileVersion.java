package com.clouddrive.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_versions", indexes = {
    @Index(name = "idx_version_file", columnList = "file_id, version_number")
})
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileItem fileItem;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 500)
    private String diskPath;

    @Column(length = 255)
    private String changeSummary;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FileVersion() {}

    public FileVersion(FileItem fileItem, int versionNumber, Long size, String diskPath, String changeSummary) {
        this.fileItem = fileItem;
        this.versionNumber = versionNumber;
        this.size = size;
        this.diskPath = diskPath;
        this.changeSummary = changeSummary;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FileItem getFileItem() { return fileItem; }
    public void setFileItem(FileItem fileItem) { this.fileItem = fileItem; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getDiskPath() { return diskPath; }
    public void setDiskPath(String diskPath) { this.diskPath = diskPath; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
