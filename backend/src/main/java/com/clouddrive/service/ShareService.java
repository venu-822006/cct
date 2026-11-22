package com.clouddrive.service;

import com.clouddrive.dto.ShareRequest;
import com.clouddrive.dto.ShareResponseDTO;
import com.clouddrive.dto.ShareWithUserRequest;
import com.clouddrive.dto.UserShareDTO;
import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.ShareLink;
import com.clouddrive.entity.User;
import com.clouddrive.entity.UserShare;
import com.clouddrive.repository.FileItemRepository;
import com.clouddrive.repository.ShareLinkRepository;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.repository.UserShareRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileItemRepository fileItemRepository;
    private final UserShareRepository userShareRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final FileStorageService fileStorageService;

    public ShareService(ShareLinkRepository shareLinkRepository,
                        FileItemRepository fileItemRepository,
                        UserShareRepository userShareRepository,
                        UserRepository userRepository,
                        ActivityService activityService,
                        FileStorageService fileStorageService) {
        this.shareLinkRepository = shareLinkRepository;
        this.fileItemRepository = fileItemRepository;
        this.userShareRepository = userShareRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.fileStorageService = fileStorageService;
    }

    private ShareResponseDTO toDTO(ShareLink link) {
        ShareResponseDTO dto = new ShareResponseDTO();
        dto.setId(link.getId());
        dto.setToken(link.getToken());
        dto.setFileId(link.getFileItem().getId());
        dto.setFileName(link.getFileItem().isDirectory() ? link.getFileItem().getName() + ".zip" : link.getFileItem().getName());
        dto.setFileSize(link.getFileItem().isDirectory() ? 0L : link.getFileItem().getSize());
        dto.setContentType(link.getFileItem().isDirectory() ? "application/zip" : link.getFileItem().getContentType());
        dto.setExpiresAt(link.getExpiresAt());
        dto.setExpired(link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now()));
        dto.setDownloadCount(link.getDownloadCount());
        dto.setCreatedAt(link.getCreatedAt());
        dto.setShareUrl("/api/share/download/" + link.getToken());
        return dto;
    }

    @Transactional
    public ShareResponseDTO createShareLink(User user, ShareRequest request) {
        FileItem fileItem = fileItemRepository.findByIdAndOwner(request.getFileId(), user)
                .orElseThrow(() -> new IllegalArgumentException("File not found or permission denied"));

        // Check if an active share link already exists
        Optional<ShareLink> existing = shareLinkRepository.findByFileItemAndCreatedBy(fileItem, user);
        if (existing.isPresent()) {
            ShareLink link = existing.get();
            if (request.getExpiresInHours() != null) {
                link.setExpiresAt(LocalDateTime.now().plusHours(request.getExpiresInHours()));
            } else {
                link.setExpiresAt(null);
            }
            ShareLink saved = shareLinkRepository.save(link);
            return toDTO(saved);
        }

        ShareLink shareLink = new ShareLink();
        shareLink.setToken(UUID.randomUUID().toString().replace("-", ""));
        shareLink.setFileItem(fileItem);
        shareLink.setCreatedBy(user);
        if (request.getExpiresInHours() != null && request.getExpiresInHours() > 0) {
            shareLink.setExpiresAt(LocalDateTime.now().plusHours(request.getExpiresInHours()));
        } else {
            shareLink.setExpiresAt(null);
        }
        shareLink.setDownloadCount(0);
        shareLink.setCreatedAt(LocalDateTime.now());

        ShareLink saved = shareLinkRepository.save(shareLink);
        activityService.log(user, "SHARE_PUBLIC", fileItem.getName(), "Generated public shareable link");
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public ShareResponseDTO getShareInfo(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found or invalid"));

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This share link has expired");
        }

        return toDTO(link);
    }

    @Transactional
    public Resource getSharedFileResource(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found or invalid"));

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This share link has expired");
        }

        FileItem fileItem = link.getFileItem();
        try {
            Path filePath = Paths.get(fileItem.getDiskPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                link.setDownloadCount(link.getDownloadCount() + 1);
                shareLinkRepository.save(link);
                return resource;
            } else {
                throw new RuntimeException("Shared file not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file URL", e);
        }
    }

    @Transactional
    public void streamSharedFolderZip(String token, OutputStream out) throws IOException {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found or invalid"));

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This share link has expired");
        }

        FileItem folder = link.getFileItem();
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Shared item is not a folder");
        }

        link.setDownloadCount(link.getDownloadCount() + 1);
        shareLinkRepository.save(link);

        fileStorageService.streamFolderAsZip(folder.getOwner(), folder.getId(), out);
    }

    @Transactional(readOnly = true)
    public FileItem getSharedFileItem(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found or invalid"));
        return link.getFileItem();
    }

    @Transactional(readOnly = true)
    public List<ShareResponseDTO> listUserShares(User user) {
        return shareLinkRepository.findByCreatedByOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void deleteShareLink(User user, Long shareId) {
        ShareLink link = shareLinkRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found"));
        if (!link.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized to revoke this share link");
        }
        shareLinkRepository.delete(link);
    }

    // ==========================================
    // Internal User-To-User Sharing Features
    // ==========================================
    @Transactional
    public UserShareDTO shareWithUser(User sender, ShareWithUserRequest request) {
        if (sender.getUsername().equalsIgnoreCase(request.getTargetUsername().trim())) {
            throw new IllegalArgumentException("Cannot share a file with yourself");
        }

        User recipient = userRepository.findByUsername(request.getTargetUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("User '" + request.getTargetUsername() + "' does not exist"));

        FileItem fileItem = fileItemRepository.findByIdAndOwner(request.getFileId(), sender)
                .orElseThrow(() -> new IllegalArgumentException("File not found or permission denied"));

        Optional<UserShare> existing = userShareRepository.findByFileItemAndSharedWith(fileItem, recipient);
        if (existing.isPresent()) {
            UserShare share = existing.get();
            share.setPermission(request.getPermission());
            UserShare saved = userShareRepository.save(share);
            return UserShareDTO.fromEntity(saved);
        }

        UserShare share = new UserShare(fileItem, sender, recipient, request.getPermission());
        UserShare saved = userShareRepository.save(share);
        activityService.log(sender, "SHARE_USER", fileItem.getName(), "Shared file with @" + recipient.getUsername());
        return UserShareDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<UserShareDTO> listSharedWithMe(User user) {
        return userShareRepository.findBySharedWithOrderByCreatedAtDesc(user)
                .stream()
                .map(UserShareDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserShareDTO> listSharedByMe(User user) {
        return userShareRepository.findBySharedByOrderByCreatedAtDesc(user)
                .stream()
                .map(UserShareDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void revokeUserShare(User sender, Long shareId) {
        UserShare share = userShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));
        if (!share.getSharedBy().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("Unauthorized to revoke this share");
        }
        userShareRepository.delete(share);
    }

    @Transactional(readOnly = true)
    public Resource loadSharedWithMeResource(User user, Long userShareId) {
        UserShare share = userShareRepository.findById(userShareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        if (!share.getSharedWith().getId().equals(user.getId()) && !share.getSharedBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Permission denied");
        }

        try {
            Path path = Paths.get(share.getFileItem().getDiskPath()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file URL", e);
        }
    }
}
