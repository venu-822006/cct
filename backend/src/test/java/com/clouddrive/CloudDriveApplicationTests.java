package com.clouddrive;

import com.clouddrive.dto.*;
import com.clouddrive.entity.User;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.security.JwtTokenProvider;
import com.clouddrive.security.RateLimitingService;
import com.clouddrive.service.AuthService;
import com.clouddrive.service.FileStorageService;
import com.clouddrive.service.FileViewerService;
import com.clouddrive.service.ShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CloudDriveApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileViewerService fileViewerService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

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

    @Test
    void testPublicShareLinkForFileAndFolder() throws Exception {
        String u = "pubshare_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);
        User user = authService.getUserByUsername(u);

        // 1. Create Folder and files
        FileItemDTO folder = fileStorageService.createFolder(user, "/", "SharedFolder");
        fileStorageService.createBlankFile(user, "/SharedFolder", "readme.txt", "Public documentation content");

        // 2. Create Public Share for Folder
        ShareRequest folderShareReq = new ShareRequest();
        folderShareReq.setFileId(folder.getId());
        folderShareReq.setExpiresInHours(48);
        ShareResponseDTO folderShare = shareService.createShareLink(user, folderShareReq);
        assertNotNull(folderShare.getToken());
        assertEquals("SharedFolder.zip", folderShare.getFileName());
        assertEquals("application/zip", folderShare.getContentType());

        // 3. Test getShareInfo for folder
        ShareResponseDTO info = shareService.getShareInfo(folderShare.getToken());
        assertEquals(folderShare.getToken(), info.getToken());
        assertFalse(info.isExpired());

        // 4. Test zip streaming for public folder share
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        shareService.streamSharedFolderZip(folderShare.getToken(), baos);
        assertTrue(baos.size() > 50);

        // 5. Create Public Share for File
        FileItemDTO singleFile = fileStorageService.createBlankFile(user, "/", "single.txt", "Direct file content");
        ShareRequest fileShareReq = new ShareRequest();
        fileShareReq.setFileId(singleFile.getId());
        ShareResponseDTO fileShare = shareService.createShareLink(user, fileShareReq);
        assertNotNull(fileShare.getToken());
        assertEquals("single.txt", fileShare.getFileName());

        org.springframework.core.io.Resource res = shareService.getSharedFileResource(fileShare.getToken());
        assertNotNull(res);
        assertTrue(res.exists());
    }

    @Test
    void testRateLimiting() {
        String testIp = "192.168.1.100";
        rateLimitingService.reset(testIp);

        // Should allow 10 login attempts
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitingService.allowLogin(testIp), "Attempt " + (i + 1) + " should be allowed");
        }

        // 11th attempt must be rejected by rate limiting
        assertFalse(rateLimitingService.allowLogin(testIp), "11th attempt should be blocked");

        // Reset clears the limit
        rateLimitingService.reset(testIp);
        assertTrue(rateLimitingService.allowLogin(testIp), "Attempt after reset should be allowed");
    }

    @Test
    void testPathTraversalBlocked() {
        String u = "sec_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);
        User user = authService.getUserByUsername(u);

        // Relative path traversal in folder name
        assertThrows(SecurityException.class, () ->
                fileStorageService.createFolder(user, "/", "../escape"));

        // Path traversal in virtual path
        assertThrows(SecurityException.class, () ->
                fileStorageService.createFolder(user, "/../../etc", "folder"));

        // Path traversal in blank file
        assertThrows(SecurityException.class, () ->
                fileStorageService.createBlankFile(user, "/", "../evil.txt", "content"));
    }

    @Test
    void testFolderZipIsolation() throws Exception {
        String u = "iso_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);
        User user = authService.getUserByUsername(u);

        // Create target folder: /Projects
        FileItemDTO targetFolder = fileStorageService.createFolder(user, "/", "Projects");
        fileStorageService.createBlankFile(user, "/Projects", "public_doc.txt", "Legitimate file");

        // Create sibling folder: /Projects-Secret
        fileStorageService.createFolder(user, "/", "Projects-Secret");
        fileStorageService.createBlankFile(user, "/Projects-Secret", "secret_passwords.txt", "CONFIDENTIAL");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fileStorageService.streamFolderAsZip(user, targetFolder.getId(), baos);

        // Read generated ZIP entries
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(baos.toByteArray()));
        java.util.zip.ZipEntry entry;
        boolean foundPublic = false;
        boolean leakedSecret = false;

        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().contains("public_doc.txt")) foundPublic = true;
            if (entry.getName().contains("secret_passwords.txt") || entry.getName().contains("Projects-Secret")) {
                leakedSecret = true;
            }
        }

        assertTrue(foundPublic, "ZIP archive must include public_doc.txt from inside the folder");
        assertFalse(leakedSecret, "ZIP archive must NOT leak files from sibling folder /Projects-Secret");
    }

    @Test
    void testFolderRenameAndMoveRecursivelyUpdatesDescendants() {
        String u = "tree_" + uid();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(u);
        req.setEmail(u + "@test.com");
        req.setPassword("pass1234");
        authService.register(req);
        User user = authService.getUserByUsername(u);

        // 1. Create Folder /Alpha and child /Alpha/child.txt
        FileItemDTO alpha = fileStorageService.createFolder(user, "/", "Alpha");
        fileStorageService.createBlankFile(user, "/Alpha", "child.txt", "Content");

        // 2. Rename /Alpha -> /Beta
        fileStorageService.rename(user, alpha.getId(), "Beta");

        // 3. Child's path must now be /Beta
        List<FileItemDTO> betaFiles = fileStorageService.listFiles(user, "/Beta");
        assertTrue(betaFiles.stream().anyMatch(f -> f.getName().equals("child.txt")),
                "Renaming folder must recursively update child's path to /Beta");

        // 4. Create subfolder /Beta/Sub and move it to /
        FileItemDTO sub = fileStorageService.createFolder(user, "/Beta", "Sub");
        fileStorageService.createBlankFile(user, "/Beta/Sub", "deep.txt", "Deep content");
        fileStorageService.move(user, sub.getId(), "/");

        List<FileItemDTO> movedDeepFiles = fileStorageService.listFiles(user, "/Sub");
        assertTrue(movedDeepFiles.stream().anyMatch(f -> f.getName().equals("deep.txt")),
                "Moving folder must recursively update nested children's paths to /Sub");
    }

    @Test
    void testListAllFilesAcrossFolders() {
        String u1 = "allfiles_" + uid();
        String u2 = "other_" + uid();

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

        User user1 = authService.getUserByUsername(u1);
        User user2 = authService.getUserByUsername(u2);

        fileStorageService.createFolder(user1, "/", "Reports");
        fileStorageService.createBlankFile(user1, "/", "root.txt", "root");
        fileStorageService.createBlankFile(user1, "/Reports", "nested.txt", "nested");
        FileItemDTO trashed = fileStorageService.createBlankFile(user1, "/", "deleted.txt", "bye");
        fileStorageService.moveToTrash(user1, trashed.getId());

        fileStorageService.createBlankFile(user2, "/", "private.txt", "secret");

        List<FileItemDTO> allFiles = fileStorageService.listAllFiles(user1);
        assertEquals(2, allFiles.size());
        assertTrue(allFiles.stream().anyMatch(f -> f.getName().equals("root.txt") && f.getPath().equals("/")));
        assertTrue(allFiles.stream().anyMatch(f -> f.getName().equals("nested.txt") && f.getPath().equals("/Reports")));
        assertFalse(allFiles.stream().anyMatch(f -> f.getName().equals("deleted.txt")));
        assertFalse(allFiles.stream().anyMatch(f -> f.getName().equals("private.txt")));
    }

    @Test
    void testUniversalFileViewerDocxCsvXlsxZip() throws Exception {
        String u = "viewer_" + uid();
        RegisterRequest r = new RegisterRequest();
        r.setUsername(u);
        r.setEmail(u + "@test.com");
        r.setPassword("pass1234");
        authService.register(r);
        User user = authService.getUserByUsername(u);

        // 1. Create valid minimal DOCX in memory
        ByteArrayOutputStream docxBaos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(docxBaos)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
            String docXml = """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Project Summary</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Hello from Cloud Drive DOCX viewer.</w:t></w:r></w:p>
                    <w:sdt><w:sdtContent><w:p><w:r><w:t>Inside SDT</w:t></w:r></w:p></w:sdtContent></w:sdt>
                    <w:tbl><w:tr><w:tc><w:p><w:r><w:t>Col1</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>Col2</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
                  </w:body>
                </w:document>
                """;
            zos.write(docXml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        MockMultipartFile docxFile = new MockMultipartFile("files", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBaos.toByteArray());

        // 2. Create CSV file
        MockMultipartFile csvFile = new MockMultipartFile("files", "metrics.csv",
                "text/csv", "Name,Score\nAlice,95\n\"Bob, Jr\",88\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 3. Create minimal XLSX
        ByteArrayOutputStream xlsxBaos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(xlsxBaos)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("xl/sharedStrings.xml"));
            zos.write("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><si><t>Item</t></si><si><t>Cloud</t></si></sst>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry("xl/workbook.xml"));
            zos.write("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Inventory\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"));
            zos.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"C1\"><v>42</v></c></row><row r=\"2\"><c r=\"A2\" t=\"s\"><v>1</v></c></row></sheetData></worksheet>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        MockMultipartFile xlsxFile = new MockMultipartFile("files", "sheet.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBaos.toByteArray());

        List<FileItemDTO> uploaded = fileStorageService.uploadFiles(user, "/", new MockMultipartFile[]{docxFile, csvFile, xlsxFile});

        FileViewerDTO docxViewer = fileViewerService.buildViewerData(user, uploaded.get(0).getId());
        assertEquals("docx", docxViewer.getKind());
        assertEquals(4, docxViewer.getBlocks().size());
        assertEquals("heading", docxViewer.getBlocks().get(0).getType());
        assertEquals("Project Summary", docxViewer.getBlocks().get(0).getText());
        assertEquals("Inside SDT", docxViewer.getBlocks().get(2).getText());
        assertEquals("table", docxViewer.getBlocks().get(3).getType());
        assertEquals(List.of("Col1", "Col2"), docxViewer.getBlocks().get(3).getRows().get(0));

        FileViewerDTO csvViewer = fileViewerService.buildViewerData(user, uploaded.get(1).getId());
        assertEquals("table", csvViewer.getKind());
        assertEquals(3, csvViewer.getSheets().get(0).getRows().size());
        assertEquals("Bob, Jr", csvViewer.getSheets().get(0).getRows().get(2).get(0));

        FileViewerDTO xlsxViewer = fileViewerService.buildViewerData(user, uploaded.get(2).getId());
        assertEquals("table", xlsxViewer.getKind());
        assertEquals("Inventory", xlsxViewer.getSheets().get(0).getName());
        assertEquals(List.of("Item", "", "42"), xlsxViewer.getSheets().get(0).getRows().get(0));
        assertEquals(List.of("Cloud", "", ""), xlsxViewer.getSheets().get(0).getRows().get(1));

        // 4. Verify HTTP REST endpoints /api/files/all and /api/files/viewer/{id}
        String u2 = "stranger_" + uid();
        RegisterRequest r2 = new RegisterRequest();
        r2.setUsername(u2);
        r2.setEmail(u2 + "@test.com");
        r2.setPassword("pass1234");
        AuthResponse u2Auth = authService.register(r2);

        String jwt = jwtTokenProvider.generateTokenFromUsername(user.getUsername());

        mockMvc.perform(get("/api/files/all"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/files/all")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/files/viewer/" + uploaded.get(0).getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("docx"))
                .andExpect(jsonPath("$.blocks[0].text").value("Project Summary"));

        // Stranger must not access another user's file via viewer API
        mockMvc.perform(get("/api/files/viewer/" + uploaded.get(0).getId())
                        .header("Authorization", "Bearer " + u2Auth.getToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testTrashRestoreDeletePermanentlyAndEmptyWithShares() throws Exception {
        String u1 = "trashowner_" + uid();
        String u2 = "trashguest_" + uid();

        RegisterRequest r1 = new RegisterRequest();
        r1.setUsername(u1);
        r1.setEmail(u1 + "@test.com");
        r1.setPassword("pass1234");
        AuthResponse ownerAuth = authService.register(r1);

        RegisterRequest r2 = new RegisterRequest();
        r2.setUsername(u2);
        r2.setEmail(u2 + "@test.com");
        r2.setPassword("pass1234");
        authService.register(r2);

        User owner = authService.getUserByUsername(u1);
        String jwt = ownerAuth.getToken();

        // 1. Create folder + nested file, plus shared file with version & public link
        FileItemDTO folder = fileStorageService.createFolder(owner, "/", "TempDir");
        FileItemDTO nested = fileStorageService.createBlankFile(owner, "/TempDir", "nested.txt", "Nested body");
        FileItemDTO sharedFile = fileStorageService.createBlankFile(owner, "/", "shared_notes.txt", "v1 content");
        fileStorageService.saveFileContent(owner, sharedFile.getId(), "v2 content");

        ShareRequest pubReq = new ShareRequest();
        pubReq.setFileId(sharedFile.getId());
        shareService.createShareLink(owner, pubReq);

        ShareWithUserRequest userShareReq = new ShareWithUserRequest();
        userShareReq.setFileId(sharedFile.getId());
        userShareReq.setTargetUsername(u2);
        userShareReq.setPermission("VIEW");
        shareService.shareWithUser(owner, userShareReq);

        // 2. Trash nested file, THEN trash parent folder -> restore nested alone -> must fall back to "/"
        fileStorageService.moveToTrash(owner, nested.getId());
        fileStorageService.moveToTrash(owner, folder.getId());

        mockMvc.perform(post("/api/files/trash/restore/" + nested.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        List<FileItemDTO> rootFiles = fileStorageService.listFiles(owner, "/");
        assertTrue(rootFiles.stream().anyMatch(f -> f.getId().equals(nested.getId()) && f.getPath().equals("/")),
                "Restoring file whose parent folder is still trashed must fallback cleanly to /");

        // 3. Trash sharedFile and delete permanently via HTTP DELETE /api/files/trash/{id}
        mockMvc.perform(delete("/api/files/" + sharedFile.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/files/trash/" + sharedFile.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // 4. Trash nested + empty Trash completely via HTTP DELETE /api/files/trash/empty
        mockMvc.perform(delete("/api/files/" + nested.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/files/trash/empty")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        assertTrue(fileStorageService.listTrash(owner).isEmpty(), "Trash should be completely empty after emptyTrash");
        assertEquals(0L, fileStorageService.getQuota(owner).getUsedBytes(), "Used bytes must reset to 0");
    }
}
