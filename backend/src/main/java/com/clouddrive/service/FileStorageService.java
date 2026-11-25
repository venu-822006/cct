package com.clouddrive.service;

import com.clouddrive.dto.*;
import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.FileVersion;
import com.clouddrive.entity.User;
import com.clouddrive.repository.FileItemRepository;
import com.clouddrive.repository.FileVersionRepository;
import com.clouddrive.repository.ShareLinkRepository;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileStorageService {

    private final FileItemRepository fileItemRepository;
    private final UserRepository userRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final FileVersionRepository fileVersionRepository;
    private final ActivityService activityService;
    private final StorageProvider storageProvider;

    @Value("${app.storage.base-dir:./storage_root}")
    private String baseDir;

    @Value("${app.storage.default-quota-bytes:524288000}")
    private long defaultQuotaBytes;

    public FileStorageService(FileItemRepository fileItemRepository,
                              UserRepository userRepository,
                              ShareLinkRepository shareLinkRepository,
                              FileVersionRepository fileVersionRepository,
                              ActivityService activityService,
                              StorageProvider storageProvider) {
        this.fileItemRepository = fileItemRepository;
        this.userRepository = userRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.fileVersionRepository = fileVersionRepository;
        this.activityService = activityService;
        this.storageProvider = storageProvider;
    }

    private Path getUserStoragePath(User user) {
        Path path = Paths.get(baseDir, "users", user.getUsername()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not create user directory: " + path, e);
        }
        return path;
    }

    private void validatePathInsideUserRoot(Path target, Path userRoot) {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Path normalizedRoot = userRoot.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new SecurityException("Directory traversal attempt detected: destination " + normalizedTarget + " is outside user root " + normalizedRoot);
        }
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        String clean = StringUtils.cleanPath(filename.trim());
        if (clean.contains("..") || clean.contains("/") || clean.contains("\\") || clean.contains("\0")) {
            throw new SecurityException("Filename contains forbidden path traversal sequence: " + clean);
        }
        return clean;
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0 && idx < filename.length() - 1) ? filename.substring(idx + 1).toLowerCase() : "";
    }

    private String normalizeVirtualPath(String path) {
        if (!StringUtils.hasText(path) || path.trim().equals("/")) {
            return "/";
        }
        String p = path.trim().replace('\\', '/');
        if (p.contains("..")) {
            throw new SecurityException("Virtual path cannot contain directory traversal sequence '..'");
        }
        if (!p.startsWith("/")) p = "/" + p;
        if (p.endsWith("/") && p.length() > 1) p = p.substring(0, p.length() - 1);
        return p;
    }

    @Transactional(readOnly = true)
    public List<FileItemDTO> listFiles(User user, String path) {
        String normalizedPath = normalizeVirtualPath(path);
        return fileItemRepository.findByOwnerAndPathAndIsTrashedFalseOrderByIdDesc(user, normalizedPath)
                .stream()
                .map(FileItemDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileItemDTO> listTrash(User user) {
        return fileItemRepository.findByOwnerAndIsTrashedTrueOrderByTrashedAtDesc(user)
                .stream()
                .map(FileItemDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileItemDTO> listStarred(User user) {
        return fileItemRepository.findByOwnerAndIsStarredTrueAndIsTrashedFalseOrderByUpdatedAtDesc(user)
                .stream()
                .map(FileItemDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileItemDTO> listRecent(User user) {
        return fileItemRepository.findTop20ByOwnerAndIsTrashedFalseAndIsDirectoryFalseOrderByUpdatedAtDesc(user)
                .stream()
                .map(FileItemDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileItemDTO> search(User user, String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return fileItemRepository.findByOwnerAndNameContainingIgnoreCaseAndIsTrashedFalse(user, query.trim())
                .stream()
                .map(FileItemDTO::fromEntity)
                .toList();
    }

    @Transactional
    public FileItemDTO createFolder(User user, String path, String folderName) {
        String normalizedPath = normalizeVirtualPath(path);
        String cleanName = sanitizeFilename(folderName.trim());

        Optional<FileItem> existing = fileItemRepository.findByOwnerAndPathAndNameAndIsTrashedFalse(user, normalizedPath, cleanName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A folder or file with name '" + cleanName + "' already exists in this folder.");
        }

        Path userDir = getUserStoragePath(user);
        String relativeSubdir = (normalizedPath.equals("/") ? "" : normalizedPath.substring(1) + "/") + cleanName;
        Path folderDiskPath = userDir.resolve(relativeSubdir).normalize();
        validatePathInsideUserRoot(folderDiskPath, userDir);

        try {
            Files.createDirectories(folderDiskPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create physical directory", e);
        }

        FileItem folderItem = new FileItem();
        folderItem.setName(cleanName);
        folderItem.setPath(normalizedPath);
        folderItem.setDirectory(true);
        folderItem.setSize(0L);
        folderItem.setContentType("application/x-directory");
        folderItem.setExtension("");
        folderItem.setColor("#3b82f6");
        folderItem.setDiskPath(folderDiskPath.toString());
        folderItem.setOwner(user);
        folderItem.setCreatedAt(LocalDateTime.now());
        folderItem.setUpdatedAt(LocalDateTime.now());

        FileItem saved = fileItemRepository.save(folderItem);
        activityService.log(user, "CREATE_FOLDER", cleanName, "Created folder at " + normalizedPath);
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public FileItemDTO createBlankFile(User user, String path, String filename, String content) {
        String normalizedPath = normalizeVirtualPath(path);
        String cleanName = sanitizeFilename(filename.trim());

        Optional<FileItem> existing = fileItemRepository.findByOwnerAndPathAndNameAndIsTrashedFalse(user, normalizedPath, cleanName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A file with name '" + cleanName + "' already exists.");
        }

        byte[] contentBytes = (content != null ? content : "").getBytes(StandardCharsets.UTF_8);
        long incomingSize = contentBytes.length;

        Long rawCurrentUsed = fileItemRepository.sumUsedBytesByOwner(user);
        long currentUsed = rawCurrentUsed != null ? rawCurrentUsed : 0L;
        long quota = user.getQuotaBytes() > 0 ? user.getQuotaBytes() : defaultQuotaBytes;

        if (currentUsed + incomingSize > quota) {
            throw new IllegalArgumentException("Storage quota exceeded! Available space: "
                    + ((quota - currentUsed) / (1024 * 1024)) + " MB.");
        }

        String uniquePrefix = UUID.randomUUID().toString().substring(0, 8);
        String storageKey = "users/" + user.getUsername() + "/" + uniquePrefix + "_" + cleanName;
        String storedKey = storageProvider.store(storageKey, contentBytes, "text/plain");

        FileItem item = new FileItem();
        item.setName(cleanName);
        item.setPath(normalizedPath);
        item.setDirectory(false);
        item.setSize(incomingSize);
        item.setContentType("text/plain");
        item.setExtension(extractExtension(cleanName));
        item.setDiskPath(storedKey);
        item.setOwner(user);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        FileItem saved = fileItemRepository.save(item);
        activityService.log(user, "CREATE_DOC", cleanName, "Created new file in " + normalizedPath);
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public List<FileItemDTO> uploadFiles(User user, String path, MultipartFile[] files) {
        String normalizedPath = normalizeVirtualPath(path);
        Path userDir = getUserStoragePath(user);

        long incomingTotalSize = 0;
        for (MultipartFile file : files) {
            incomingTotalSize += file.getSize();
        }

        Long rawCurrentUsed = fileItemRepository.sumUsedBytesByOwner(user);
        long currentUsed = rawCurrentUsed != null ? rawCurrentUsed : 0L;
        long quota = user.getQuotaBytes() > 0 ? user.getQuotaBytes() : defaultQuotaBytes;

        if (currentUsed + incomingTotalSize > quota) {
            throw new IllegalArgumentException("Storage quota exceeded! Available space: "
                    + ((quota - currentUsed) / (1024 * 1024)) + " MB.");
        }

        List<FileItemDTO> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = sanitizeFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file_" + System.currentTimeMillis());

            // Handle potential versioning if file already exists in same folder
            Optional<FileItem> existing = fileItemRepository.findByOwnerAndPathAndNameAndIsTrashedFalse(user, normalizedPath, originalName);
            if (existing.isPresent()) {
                FileItem existingItem = existing.get();
                // Backup existing version
                FileVersion version = new FileVersion(
                        existingItem,
                        existingItem.getVersionCount(),
                        existingItem.getSize(),
                        existingItem.getDiskPath(),
                        "Replaced by new upload"
                );
                fileVersionRepository.save(version);

                // Write new content
                String uniquePrefix = UUID.randomUUID().toString().substring(0, 8);
                String storageKey = "users/" + user.getUsername() + "/" + uniquePrefix + "_" + originalName;
                try {
                    String storedKey = storageProvider.store(storageKey, file.getInputStream(), file.getSize(), file.getContentType());
                    existingItem.setDiskPath(storedKey);
                    existingItem.setSize(file.getSize());
                    existingItem.setVersionCount(existingItem.getVersionCount() + 1);
                    existingItem.setUpdatedAt(LocalDateTime.now());
                    FileItem saved = fileItemRepository.save(existingItem);
                    uploaded.add(FileItemDTO.fromEntity(saved));
                    activityService.log(user, "UPLOAD_VERSION", originalName, "Uploaded new version (v" + saved.getVersionCount() + ")");
                    continue;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save new upload", e);
                }
            }

            String uniquePrefix = UUID.randomUUID().toString().substring(0, 8);
            String storageKey = "users/" + user.getUsername() + "/" + uniquePrefix + "_" + originalName;
            String storedKey;
            try {
                storedKey = storageProvider.store(storageKey, file.getInputStream(), file.getSize(), file.getContentType());
            } catch (IOException e) {
                throw new RuntimeException("Could not store file " + originalName, e);
            }

            FileItem item = new FileItem();
            item.setName(originalName);
            item.setPath(normalizedPath);
            item.setDirectory(false);
            item.setSize(file.getSize());
            item.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            item.setExtension(extractExtension(originalName));
            item.setDiskPath(storedKey);
            item.setOwner(user);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());

            FileItem saved = fileItemRepository.save(item);
            uploaded.add(FileItemDTO.fromEntity(saved));
            activityService.log(user, "UPLOAD", originalName, "Uploaded file (" + (file.getSize() / 1024) + " KB) to " + normalizedPath);
        }

        user.setUsedBytes(currentUsed + incomingTotalSize);
        userRepository.save(user);

        return uploaded;
    }

    @Transactional(readOnly = true)
    public Resource loadFileAsResource(User user, Long fileId) {
        FileItem item = fileItemRepository.findByIdAndOwner(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found or permission denied"));

        Resource resource = storageProvider.loadAsResource(item.getDiskPath());
        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("File not found on storage server");
        }
    }

    @Transactional(readOnly = true)
    public FileItem getFileItem(User user, Long fileId) {
        return fileItemRepository.findByIdAndOwner(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    @Transactional(readOnly = true)
    public FileContentDTO getFileContent(User user, Long fileId) {
        FileItem item = getFileItem(user, fileId);
        if (item.isDirectory()) {
            throw new IllegalArgumentException("Cannot open folder as text");
        }
        try (InputStream is = storageProvider.loadAsStream(item.getDiskPath())) {
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new FileContentDTO(item.getId(), item.getName(), text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    @Transactional
    public FileItemDTO saveFileContent(User user, Long fileId, String content) {
        FileItem item = getFileItem(user, fileId);
        byte[] newContentBytes = (content != null ? content : "").getBytes(StandardCharsets.UTF_8);
        long newSize = newContentBytes.length;
        long delta = newSize - item.getSize();
        if (delta > 0) {
            Long rawCurrentUsed = fileItemRepository.sumUsedBytesByOwner(user);
            long currentUsed = rawCurrentUsed != null ? rawCurrentUsed : 0L;
            long quota = user.getQuotaBytes() > 0 ? user.getQuotaBytes() : defaultQuotaBytes;
            if (currentUsed + delta > quota) {
                throw new IllegalArgumentException("Storage quota exceeded! Cannot save modified content.");
            }
        }

        try {
            // Backup existing version
            FileVersion version = new FileVersion(
                    item,
                    item.getVersionCount(),
                    item.getSize(),
                    item.getDiskPath(),
                    "Edited via in-browser editor"
            );
            fileVersionRepository.save(version);

            // Create new storage object for edited version to preserve previous version history
            String uniquePrefix = UUID.randomUUID().toString().substring(0, 8);
            String storageKey = "users/" + user.getUsername() + "/" + uniquePrefix + "_" + item.getName();
            String storedKey = storageProvider.store(storageKey, newContentBytes, item.getContentType() != null ? item.getContentType() : "text/plain");

            item.setDiskPath(storedKey);
            item.setSize(newSize);
            item.setVersionCount(item.getVersionCount() + 1);
            item.setUpdatedAt(LocalDateTime.now());
            FileItem saved = fileItemRepository.save(item);

            activityService.log(user, "EDIT", item.getName(), "Saved modified content (v" + saved.getVersionCount() + ")");
            return FileItemDTO.fromEntity(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file content", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FileVersionDTO> listVersions(User user, Long fileId) {
        FileItem item = getFileItem(user, fileId);
        return fileVersionRepository.findByFileItemOrderByVersionNumberDesc(item)
                .stream()
                .map(FileVersionDTO::fromEntity)
                .toList();
    }

    @Transactional
    public FileItemDTO restoreVersion(User user, Long fileId, Long versionId) {
        FileItem item = getFileItem(user, fileId);
        FileVersion version = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        if (!version.getFileItem().getId().equals(item.getId())) {
            throw new IllegalArgumentException("Version does not belong to this file");
        }

        // Backup current before restoring
        FileVersion currentBackup = new FileVersion(
                item,
                item.getVersionCount(),
                item.getSize(),
                item.getDiskPath(),
                "Before restoring v" + version.getVersionNumber()
        );
        fileVersionRepository.save(currentBackup);

        item.setDiskPath(version.getDiskPath());
        item.setSize(version.getSize());
        item.setVersionCount(item.getVersionCount() + 1);
        item.setUpdatedAt(LocalDateTime.now());

        FileItem saved = fileItemRepository.save(item);
        activityService.log(user, "RESTORE_VERSION", item.getName(), "Restored to version " + version.getVersionNumber());
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public FileItemDTO updateMetadata(User user, Long fileId, String color, String tags) {
        FileItem item = getFileItem(user, fileId);
        if (color != null) item.setColor(color.trim());
        if (tags != null) item.setTags(tags.trim());
        item.setUpdatedAt(LocalDateTime.now());
        FileItem saved = fileItemRepository.save(item);
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public FileItemDTO rename(User user, Long fileId, String newName) {
        FileItem item = getFileItem(user, fileId);
        String cleanName = sanitizeFilename(newName.trim());
        String oldName = item.getName();
        if (cleanName.equals(oldName)) {
            return FileItemDTO.fromEntity(item);
        }

        Optional<FileItem> existing = fileItemRepository.findByOwnerAndPathAndNameAndIsTrashedFalse(user, item.getPath(), cleanName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("An item with name '" + cleanName + "' already exists in this folder.");
        }

        String oldVirtualFolder = (item.getPath().equals("/") ? "" : item.getPath()) + "/" + oldName;
        String newVirtualFolder = (item.getPath().equals("/") ? "" : item.getPath()) + "/" + cleanName;

        item.setName(cleanName);
        if (!item.isDirectory()) {
            item.setExtension(extractExtension(cleanName));
        }
        item.setUpdatedAt(LocalDateTime.now());
        FileItem saved = fileItemRepository.save(item);

        if (item.isDirectory()) {
            List<FileItem> descendants = fileItemRepository.findByOwnerAndPathStartingWith(user, oldVirtualFolder);
            for (FileItem d : descendants) {
                if (d.getPath().equals(oldVirtualFolder)) {
                    d.setPath(newVirtualFolder);
                } else if (d.getPath().startsWith(oldVirtualFolder + "/")) {
                    String sub = d.getPath().substring(oldVirtualFolder.length());
                    d.setPath(newVirtualFolder + sub);
                }
                d.setUpdatedAt(LocalDateTime.now());
                fileItemRepository.save(d);
            }
        }

        activityService.log(user, "RENAME", cleanName, "Renamed from '" + oldName + "' to '" + cleanName + "'");
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public FileItemDTO move(User user, Long fileId, String targetPath) {
        FileItem item = getFileItem(user, fileId);
        String oldPath = item.getPath();
        String normalizedTarget = normalizeVirtualPath(targetPath);
        if (oldPath.equals(normalizedTarget)) {
            return FileItemDTO.fromEntity(item);
        }

        Optional<FileItem> existing = fileItemRepository.findByOwnerAndPathAndNameAndIsTrashedFalse(user, normalizedTarget, item.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("An item with name '" + item.getName() + "' already exists in target destination.");
        }

        if (item.isDirectory()) {
            String folderFullPath = (oldPath.equals("/") ? "" : oldPath) + "/" + item.getName();
            if (normalizedTarget.equals(folderFullPath) || normalizedTarget.startsWith(folderFullPath + "/")) {
                throw new IllegalArgumentException("Cannot move a folder into itself or its own subfolder.");
            }
        }

        String oldVirtualFolder = (oldPath.equals("/") ? "" : oldPath) + "/" + item.getName();
        String newVirtualFolder = (normalizedTarget.equals("/") ? "" : normalizedTarget) + "/" + item.getName();

        item.setPath(normalizedTarget);
        item.setUpdatedAt(LocalDateTime.now());
        FileItem saved = fileItemRepository.save(item);

        if (item.isDirectory()) {
            List<FileItem> descendants = fileItemRepository.findByOwnerAndPathStartingWith(user, oldVirtualFolder);
            for (FileItem d : descendants) {
                if (d.getPath().equals(oldVirtualFolder)) {
                    d.setPath(newVirtualFolder);
                } else if (d.getPath().startsWith(oldVirtualFolder + "/")) {
                    String sub = d.getPath().substring(oldVirtualFolder.length());
                    d.setPath(newVirtualFolder + sub);
                }
                d.setUpdatedAt(LocalDateTime.now());
                fileItemRepository.save(d);
            }
        }

        activityService.log(user, "MOVE", item.getName(), "Moved from " + oldPath + " to " + normalizedTarget);
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public FileItemDTO toggleStar(User user, Long fileId) {
        FileItem item = getFileItem(user, fileId);
        item.setStarred(!item.isStarred());
        item.setUpdatedAt(LocalDateTime.now());
        FileItem saved = fileItemRepository.save(item);
        activityService.log(user, "STAR", item.getName(), (item.isStarred() ? "Starred" : "Unstarred") + " file");
        return FileItemDTO.fromEntity(saved);
    }

    @Transactional
    public void moveToTrash(User user, Long fileId) {
        FileItem item = getFileItem(user, fileId);
        item.setTrashed(true);
        item.setOriginalPath(item.getPath());
        item.setTrashedAt(LocalDateTime.now());
        fileItemRepository.save(item);

        if (item.isDirectory()) {
            String folderVirtualPath = (item.getPath().equals("/") ? "" : item.getPath()) + "/" + item.getName();
            List<FileItem> descendants = fileItemRepository.findByOwnerAndPathStartingWith(user, folderVirtualPath);
            for (FileItem d : descendants) {
                if (d.getPath().equals(folderVirtualPath) || d.getPath().startsWith(folderVirtualPath + "/")) {
                    d.setTrashed(true);
                    d.setOriginalPath(d.getPath());
                    d.setTrashedAt(LocalDateTime.now());
                    fileItemRepository.save(d);
                }
            }
        }

        activityService.log(user, "TRASH", item.getName(), "Moved to Recycle Bin");
    }

    @Transactional
    public void restoreFromTrash(User user, Long fileId) {
        FileItem item = fileItemRepository.findByIdAndOwner(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        item.setTrashed(false);
        if (StringUtils.hasText(item.getOriginalPath())) {
            item.setPath(item.getOriginalPath());
        }
        item.setTrashedAt(null);
        fileItemRepository.save(item);

        if (item.isDirectory()) {
            String folderVirtualPath = (item.getPath().equals("/") ? "" : item.getPath()) + "/" + item.getName();
            List<FileItem> descendants = fileItemRepository.findByOwnerAndPathStartingWith(user, folderVirtualPath);
            for (FileItem d : descendants) {
                if (d.getPath().equals(folderVirtualPath) || d.getPath().startsWith(folderVirtualPath + "/")) {
                    d.setTrashed(false);
                    if (StringUtils.hasText(d.getOriginalPath())) {
                        d.setPath(d.getOriginalPath());
                    }
                    d.setTrashedAt(null);
                    fileItemRepository.save(d);
                }
            }
        }

        activityService.log(user, "RESTORE", item.getName(), "Restored from Recycle Bin");
    }

    @Transactional
    public void emptyTrash(User user) {
        List<FileItem> trashed = fileItemRepository.findByOwnerAndIsTrashedTrueOrderByTrashedAtDesc(user);
        for (FileItem item : trashed) {
            shareLinkRepository.deleteByFileItem(item);
            fileVersionRepository.deleteByFileItem(item);
            if (!item.isDirectory() && StringUtils.hasText(item.getDiskPath())) {
                storageProvider.delete(item.getDiskPath());
            }
            fileItemRepository.delete(item);
        }
        Long newUsed = fileItemRepository.sumUsedBytesByOwner(user);
        user.setUsedBytes(newUsed != null ? newUsed : 0L);
        userRepository.save(user);

        activityService.log(user, "EMPTY_TRASH", "Recycle Bin", "Emptied trash permanently");
    }

    @Transactional
    public void executeBatchAction(User user, BatchActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) return;

        for (Long id : request.getIds()) {
            try {
                if ("TRASH".equalsIgnoreCase(request.getAction())) {
                    moveToTrash(user, id);
                } else if ("STAR".equalsIgnoreCase(request.getAction())) {
                    FileItem item = getFileItem(user, id);
                    item.setStarred(true);
                    fileItemRepository.save(item);
                } else if ("UNSTAR".equalsIgnoreCase(request.getAction())) {
                    FileItem item = getFileItem(user, id);
                    item.setStarred(false);
                    fileItemRepository.save(item);
                } else if ("RESTORE".equalsIgnoreCase(request.getAction())) {
                    restoreFromTrash(user, id);
                } else if ("MOVE".equalsIgnoreCase(request.getAction()) && request.getTargetPath() != null) {
                    move(user, id, request.getTargetPath());
                }
            } catch (Exception ignored) {}
        }
    }

    public void streamFolderAsZip(User user, Long folderId, OutputStream out) throws IOException {
        FileItem folder = getFileItem(user, folderId);
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Item is not a directory");
        }

        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            String folderVirtualPath = (folder.getPath().equals("/") ? "" : folder.getPath()) + "/" + folder.getName();
            List<FileItem> rawDescendants = fileItemRepository.findByOwnerAndPathStartingWith(user, folderVirtualPath);
            List<FileItem> allDescendants = rawDescendants.stream()
                    .filter(item -> item.getPath().equals(folderVirtualPath) || item.getPath().startsWith(folderVirtualPath + "/"))
                    .toList();

            for (FileItem item : allDescendants) {
                if (item.isTrashed()) continue;
                String relativeEntryPath = item.getPath().substring(folderVirtualPath.length());
                if (relativeEntryPath.startsWith("/")) relativeEntryPath = relativeEntryPath.substring(1);
                String entryName = (relativeEntryPath.isEmpty() ? "" : relativeEntryPath + "/") + item.getName();

                if (item.isDirectory()) {
                    zos.putNextEntry(new ZipEntry(entryName + "/"));
                    zos.closeEntry();
                } else {
                    if (storageProvider.exists(item.getDiskPath())) {
                        zos.putNextEntry(new ZipEntry(entryName));
                        try (InputStream is = storageProvider.loadAsStream(item.getDiskPath())) {
                            is.transferTo(zos);
                        }
                        zos.closeEntry();
                    }
                }
            }
            zos.finish();
        }
    }

    public void streamMultipleFilesAsZip(User user, List<Long> fileIds, OutputStream out) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Long id : fileIds) {
                try {
                    FileItem item = getFileItem(user, id);
                    if (item.isTrashed()) continue;

                    if (!item.isDirectory()) {
                        if (storageProvider.exists(item.getDiskPath())) {
                            zos.putNextEntry(new ZipEntry(item.getName()));
                            try (InputStream is = storageProvider.loadAsStream(item.getDiskPath())) {
                                is.transferTo(zos);
                            }
                            zos.closeEntry();
                        }
                    }
                } catch (Exception ignored) {}
            }
            zos.finish();
        }
    }

    @Transactional(readOnly = true)
    public StorageBreakdownDTO getStorageBreakdown(User user) {
        StorageBreakdownDTO dto = new StorageBreakdownDTO();
        dto.setTotalQuotaBytes(user.getQuotaBytes());

        List<FileItem> allItems = fileItemRepository.findByOwnerAndPathStartingWith(user, "/");
        long totalUsed = 0;
        long trashBytes = 0;

        for (FileItem item : allItems) {
            if (item.isDirectory()) continue;
            long size = item.getSize() != null ? item.getSize() : 0L;

            if (item.isTrashed()) {
                trashBytes += size;
                continue;
            }

            totalUsed += size;
            String ext = item.getExtension() != null ? item.getExtension().toLowerCase() : "";

            if (Arrays.asList("pdf", "doc", "docx", "txt", "md", "csv", "xlsx", "pptx", "rtf").contains(ext)) {
                dto.setDocumentsBytes(dto.getDocumentsBytes() + size);
            } else if (Arrays.asList("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp").contains(ext)) {
                dto.setImagesBytes(dto.getImagesBytes() + size);
            } else if (Arrays.asList("mp4", "mov", "mkv", "webm", "avi", "wmv").contains(ext)) {
                dto.setVideosBytes(dto.getVideosBytes() + size);
            } else if (Arrays.asList("mp3", "wav", "flac", "aac", "ogg", "m4a").contains(ext)) {
                dto.setAudioBytes(dto.getAudioBytes() + size);
            } else if (Arrays.asList("js", "ts", "html", "css", "py", "java", "json", "xml", "sql", "sh", "rs", "cpp").contains(ext)) {
                dto.setCodeBytes(dto.getCodeBytes() + size);
            } else if (Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2").contains(ext)) {
                dto.setArchivesBytes(dto.getArchivesBytes() + size);
            } else {
                dto.setOtherBytes(dto.getOtherBytes() + size);
            }
        }

        dto.setTrashBytes(trashBytes);
        dto.setTotalUsedBytes(totalUsed);
        return dto;
    }

    @Transactional(readOnly = true)
    public QuotaDTO getQuota(User user) {
        Long used = fileItemRepository.sumUsedBytesByOwner(user);
        return new QuotaDTO(used != null ? used : 0L, user.getQuotaBytes());
    }
}
