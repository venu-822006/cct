package com.clouddrive.controller;

import com.clouddrive.dto.*;
import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.User;
import com.clouddrive.service.AuthService;
import com.clouddrive.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final AuthService authService;

    public FileController(FileStorageService fileStorageService, AuthService authService) {
        this.fileStorageService = fileStorageService;
        this.authService = authService;
    }

    private User getUser(UserDetails userDetails) {
        return authService.getUserByUsername(userDetails.getUsername());
    }

    @GetMapping
    public ResponseEntity<List<FileItemDTO>> listFiles(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestParam(defaultValue = "/") String path) {
        return ResponseEntity.ok(fileStorageService.listFiles(getUser(userDetails), path));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileItemDTO>> listTrash(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileStorageService.listTrash(getUser(userDetails)));
    }

    @GetMapping("/starred")
    public ResponseEntity<List<FileItemDTO>> listStarred(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileStorageService.listStarred(getUser(userDetails)));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<FileItemDTO>> listRecent(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileStorageService.listRecent(getUser(userDetails)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileItemDTO>> search(@AuthenticationPrincipal UserDetails userDetails,
                                                    @RequestParam String q) {
        return ResponseEntity.ok(fileStorageService.search(getUser(userDetails), q));
    }

    @GetMapping("/quota")
    public ResponseEntity<QuotaDTO> getQuota(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileStorageService.getQuota(getUser(userDetails)));
    }

    @GetMapping("/storage-breakdown")
    public ResponseEntity<StorageBreakdownDTO> getStorageBreakdown(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileStorageService.getStorageBreakdown(getUser(userDetails)));
    }

    @PostMapping("/folder")
    public ResponseEntity<?> createFolder(@AuthenticationPrincipal UserDetails userDetails,
                                          @Valid @RequestBody CreateFolderRequest request) {
        try {
            FileItemDTO created = fileStorageService.createFolder(getUser(userDetails), request.getPath(), request.getName());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/blank")
    public ResponseEntity<?> createBlankFile(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody Map<String, String> body) {
        try {
            String path = body.getOrDefault("path", "/");
            String filename = body.getOrDefault("name", "Untitled.txt");
            String content = body.getOrDefault("content", "");
            FileItemDTO created = fileStorageService.createBlankFile(getUser(userDetails), path, filename, content);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestParam(defaultValue = "/") String path,
                                         @RequestParam("files") MultipartFile[] files) {
        try {
            List<FileItemDTO> uploaded = fileStorageService.uploadFiles(getUser(userDetails), path, files);
            return ResponseEntity.ok(uploaded);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable Long id) {
        User user = getUser(userDetails);
        Resource resource = fileStorageService.loadFileAsResource(user, id);
        FileItem item = fileStorageService.getFileItem(user, id);

        String contentType = item.getContentType() != null ? item.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + item.getName() + "\"")
                .body(resource);
    }

    @GetMapping("/download-folder/{id}")
    public ResponseEntity<StreamingResponseBody> downloadFolderAsZip(@AuthenticationPrincipal UserDetails userDetails,
                                                                     @PathVariable Long id) {
        User user = getUser(userDetails);
        FileItem item = fileStorageService.getFileItem(user, id);
        String zipName = item.getName() + ".zip";

        StreamingResponseBody stream = out -> fileStorageService.streamFolderAsZip(user, id, out);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                .body(stream);
    }

    @PostMapping("/download-zip")
    public ResponseEntity<StreamingResponseBody> downloadMultipleAsZip(@AuthenticationPrincipal UserDetails userDetails,
                                                                       @RequestBody List<Long> fileIds) {
        User user = getUser(userDetails);
        StreamingResponseBody stream = out -> fileStorageService.streamMultipleFilesAsZip(user, fileIds, out);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cloud_drive_download.zip\"")
                .body(stream);
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id) {
        User user = getUser(userDetails);
        Resource resource = fileStorageService.loadFileAsResource(user, id);
        FileItem item = fileStorageService.getFileItem(user, id);

        String contentType = item.getContentType() != null ? item.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + item.getName() + "\"")
                .body(resource);
    }

    @GetMapping("/content/{id}")
    public ResponseEntity<?> getFileContent(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id) {
        try {
            FileContentDTO content = fileStorageService.getFileContent(getUser(userDetails), id);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/content/{id}")
    public ResponseEntity<?> saveFileContent(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        try {
            FileItemDTO saved = fileStorageService.saveFileContent(getUser(userDetails), id, body.get("content"));
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/versions/{id}")
    public ResponseEntity<List<FileVersionDTO>> listVersions(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(fileStorageService.listVersions(getUser(userDetails), id));
    }

    @PostMapping("/versions/{id}/restore/{versionId}")
    public ResponseEntity<?> restoreVersion(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id,
                                            @PathVariable Long versionId) {
        try {
            FileItemDTO restored = fileStorageService.restoreVersion(getUser(userDetails), id, versionId);
            return ResponseEntity.ok(restored);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/meta/{id}")
    public ResponseEntity<?> updateMetadata(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id,
                                            @RequestBody UpdateFileMetaRequest request) {
        try {
            FileItemDTO item = fileStorageService.updateMetadata(getUser(userDetails), id, request.getColor(), request.getTags());
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> executeBatchAction(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody BatchActionRequest request) {
        try {
            fileStorageService.executeBatchAction(getUser(userDetails), request);
            return ResponseEntity.ok(Map.of("message", "Batch operation executed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/rename/{id}")
    public ResponseEntity<?> rename(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id,
                                    @Valid @RequestBody RenameRequest request) {
        try {
            FileItemDTO renamed = fileStorageService.rename(getUser(userDetails), id, request.getNewName());
            return ResponseEntity.ok(renamed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/move/{id}")
    public ResponseEntity<?> move(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id,
                                  @Valid @RequestBody MoveRequest request) {
        try {
            FileItemDTO moved = fileStorageService.move(getUser(userDetails), id, request.getTargetPath());
            return ResponseEntity.ok(moved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/star/{id}")
    public ResponseEntity<?> toggleStar(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long id) {
        try {
            FileItemDTO item = fileStorageService.toggleStar(getUser(userDetails), id);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> moveToTrash(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long id) {
        try {
            fileStorageService.moveToTrash(getUser(userDetails), id);
            return ResponseEntity.ok(Map.of("message", "Item moved to trash successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trash/restore/{id}")
    public ResponseEntity<?> restoreFromTrash(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long id) {
        try {
            fileStorageService.restoreFromTrash(getUser(userDetails), id);
            return ResponseEntity.ok(Map.of("message", "Item restored successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/trash/empty")
    public ResponseEntity<?> emptyTrash(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            fileStorageService.emptyTrash(getUser(userDetails));
            return ResponseEntity.ok(Map.of("message", "Trash emptied permanently"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
