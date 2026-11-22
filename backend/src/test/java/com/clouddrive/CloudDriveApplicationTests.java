package com.clouddrive;

import com.clouddrive.dto.*;
import com.clouddrive.entity.User;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.security.JwtTokenProvider;
import com.clouddrive.service.AuthService;
import com.clouddrive.service.FileStorageService;
import com.clouddrive.service.ShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CloudDriveApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String uid() {
        return String.valueOf(System.nanoTime());
    }

    @Test
    void testUserRegistrationAndJwtIssuance() {
        String u = "user_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@example.com");
        req.setPassword("securePass123");
        req.setFullName("Test User");

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals(u, res.getUsername());
        assertTrue(jwtTokenProvider.validateToken(res.getToken()));
        assertEquals(u, jwtTokenProvider.getUsernameFromToken(res.getToken()));
    }

    @Test
    void testDuplicateUsernameRejection() {
        String u = "dup_" + uid();
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername(u);
        req1.setEmail(u + "1@test.com");
        req1.setPassword("pass1234");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername(u);
        req2.setEmail(u + "2@test.com");
        req2.setPassword("pass1234");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req2));
    }

    @Test
    void testCreateFolderAndList() {
        String u = "folder_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);

        User user = authService.getUserByUsername(u);
        FileItemDTO folder = fileStorageService.createFolder(user, "/", "Projects");

        assertNotNull(folder);
        assertTrue(folder.isDirectory());
        assertEquals("Projects", folder.getName());

        List<FileItemDTO> files = fileStorageService.listFiles(user, "/");
        assertTrue(files.stream().anyMatch(f -> f.getName().equals("Projects")));
    }

    @Test
    void testFileUploadAndTrashLifecycle() {
        String u = "up_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);

        User user = authService.getUserByUsername(u);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "Document1.pdf",
                "application/pdf",
                "Sample PDF content".getBytes()
        );

        List<FileItemDTO> uploaded = fileStorageService.uploadFiles(user, "/", new MockMultipartFile[]{file});
        assertEquals(1, uploaded.size());
        FileItemDTO item = uploaded.get(0);
        assertEquals("Document1.pdf", item.getName());

        // Test Move to Trash
        fileStorageService.moveToTrash(user, item.getId());
        List<FileItemDTO> rootFiles = fileStorageService.listFiles(user, "/");
        assertFalse(rootFiles.stream().anyMatch(f -> f.getId().equals(item.getId())));

        List<FileItemDTO> trash = fileStorageService.listTrash(user);
        assertTrue(trash.stream().anyMatch(f -> f.getId().equals(item.getId())));

        // Test Restore
        fileStorageService.restoreFromTrash(user, item.getId());
        List<FileItemDTO> restoredRoot = fileStorageService.listFiles(user, "/");
        assertTrue(restoredRoot.stream().anyMatch(f -> f.getId().equals(item.getId())));
    }

    @Test
    void testFileVersionHistory() {
        String u = "ver_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);
        User user = authService.getUserByUsername(u);

        FileItemDTO doc = fileStorageService.createBlankFile(user, "/", "notes.txt", "Version 1 Content");
        assertEquals(1, doc.getVersionCount());

        // Edit file to create version 2
        FileItemDTO edited = fileStorageService.saveFileContent(user, doc.getId(), "Version 2 Content Modified");
        assertEquals(2, edited.getVersionCount());

        List<FileVersionDTO> versions = fileStorageService.listVersions(user, doc.getId());
        assertEquals(1, versions.size());
        assertEquals(1, versions.get(0).getVersionNumber());

        // Restore version 1
        FileItemDTO restored = fileStorageService.restoreVersion(user, doc.getId(), versions.get(0).getId());
        assertEquals(3, restored.getVersionCount());
    }

    @Test
    void testUserToUserSharing() {
        String u1 = "sender_" + uid();
        String u2 = "recv_" + uid();

        RegisterRequest r1 = new RegisterRequest();
        r1.setUsername(u1);
        r1.setEmail(u1 + "@test.com");
        r1.setPassword("pass1234");
        authService.register(r1);

        RegisterRequest r2 = new RegisterRequest();
        r2.setUsername(u2);
        r2.setEmail(u2 + "@test.com");
        r2.setPassword("pass1234");
        authService.register(r2);

        User sender = authService.getUserByUsername(u1);
        User recipient = authService.getUserByUsername(u2);

        FileItemDTO file = fileStorageService.createBlankFile(sender, "/", "shared_doc.txt", "Collab text");

        ShareWithUserRequest shareReq = new ShareWithUserRequest();
        shareReq.setFileId(file.getId());
        shareReq.setTargetUsername(recipient.getUsername());
        shareReq.setPermission("VIEW");

        UserShareDTO shareResult = shareService.shareWithUser(sender, shareReq);
        assertNotNull(shareResult);
        assertEquals(u1, shareResult.getSharedByUsername());
        assertEquals(u2, shareResult.getSharedWithUsername());

        List<UserShareDTO> sharedWithMe = shareService.listSharedWithMe(recipient);
        assertTrue(sharedWithMe.stream().anyMatch(s -> s.getFileItem().getName().equals("shared_doc.txt")));
    }

    @Test
    void testStorageBreakdownAndZipStreaming() throws Exception {
        String u = "zip_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);
        User user = authService.getUserByUsername(u);

        FileItemDTO folder = fileStorageService.createFolder(user, "/", "ProjectFolder");
        fileStorageService.createBlankFile(user, "/ProjectFolder", "file1.txt", "Hello 1");
        fileStorageService.createBlankFile(user, "/ProjectFolder", "file2.txt", "Hello 2");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fileStorageService.streamFolderAsZip(user, folder.getId(), baos);
        byte[] zipBytes = baos.toByteArray();
        assertTrue(zipBytes.length > 50); // Valid zip archive header exists

        StorageBreakdownDTO breakdown = fileStorageService.getStorageBreakdown(user);
        assertNotNull(breakdown);
        assertTrue(breakdown.getTotalUsedBytes() > 0);
    }
}
