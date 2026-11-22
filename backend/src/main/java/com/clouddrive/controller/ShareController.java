package com.clouddrive.controller;

import com.clouddrive.dto.*;
import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.User;
import com.clouddrive.service.AuthService;
import com.clouddrive.service.ShareService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final ShareService shareService;
    private final AuthService authService;

    public ShareController(ShareService shareService, AuthService authService) {
        this.shareService = shareService;
        this.authService = authService;
    }

    private User getUser(UserDetails userDetails) {
        return authService.getUserByUsername(userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<?> createShare(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody ShareRequest request) {
        try {
            ShareResponseDTO response = shareService.createShareLink(getUser(userDetails), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-shares")
    public ResponseEntity<List<ShareResponseDTO>> listMyShares(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shareService.listUserShares(getUser(userDetails)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShare(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long id) {
        try {
            shareService.deleteShareLink(getUser(userDetails), id);
            return ResponseEntity.ok(Map.of("message", "Share link revoked"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Public endpoints (no authentication required)
    @GetMapping("/info/{token}")
    public ResponseEntity<?> getShareInfo(@PathVariable String token) {
        try {
            ShareResponseDTO info = shareService.getShareInfo(token);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download/{token}")
    public void downloadSharedFile(@PathVariable String token, HttpServletResponse response) throws IOException {
        try {
            FileItem fileItem = shareService.getSharedFileItem(token);
            if (fileItem.isDirectory()) {
                String zipName = fileItem.getName() + ".zip";
                response.setContentType("application/zip");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"");
                shareService.streamSharedFolderZip(token, response.getOutputStream());
                response.flushBuffer();
            } else {
                Resource resource = shareService.getSharedFileResource(token);
                String contentType = fileItem.getContentType() != null ? fileItem.getContentType() : "application/octet-stream";
                response.setContentType(contentType);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileItem.getName() + "\"");
                if (fileItem.getSize() != null && fileItem.getSize() > 0) {
                    response.setContentLengthLong(fileItem.getSize());
                }
                try (InputStream in = resource.getInputStream(); OutputStream out = response.getOutputStream()) {
                    in.transferTo(out);
                }
                response.flushBuffer();
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // User-to-User Sharing Endpoints
    @PostMapping("/user")
    public ResponseEntity<?> shareWithUser(@AuthenticationPrincipal UserDetails userDetails,
                                           @Valid @RequestBody ShareWithUserRequest request) {
        try {
            UserShareDTO share = shareService.shareWithUser(getUser(userDetails), request);
            return ResponseEntity.ok(share);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<List<UserShareDTO>> listSharedWithMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shareService.listSharedWithMe(getUser(userDetails)));
    }

    @GetMapping("/shared-by-me")
    public ResponseEntity<List<UserShareDTO>> listSharedByMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shareService.listSharedByMe(getUser(userDetails)));
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<?> revokeUserShare(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long id) {
        try {
            shareService.revokeUserShare(getUser(userDetails), id);
            return ResponseEntity.ok(Map.of("message", "Share permission revoked"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user-download/{id}")
    public ResponseEntity<?> downloadUserSharedFile(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id) {
        try {
            Resource resource = shareService.loadSharedWithMeResource(getUser(userDetails), id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shared_file\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
