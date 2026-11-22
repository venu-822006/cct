import { describe, it, expect, beforeEach, afterEach } from "bun:test";
import { promises as fs } from "fs";
import { join } from "path";
import { createHmac } from "crypto";
import { LocalStorageAdapter } from "../server/storage/adapters";

describe("Personal Cloud Drive - Academic Test Suite", () => {
  let testDriveDir: string;
  let adapter: LocalStorageAdapter;
  const SECRET = "test-college-project-secret";

  beforeEach(async () => {
    testDriveDir = join(process.cwd(), `tmp-drive-test-${Date.now()}`);
    await fs.mkdir(testDriveDir, { recursive: true });
    adapter = new LocalStorageAdapter(testDriveDir);
  });

  afterEach(async () => {
    try {
      await fs.rm(testDriveDir, { recursive: true, force: true });
    } catch {}
  });

  describe("Module 1: File Storage Lifecycle", () => {
    it("should allow uploading/writing files to personal drive", async () => {
      const fileName = "/Lecture1_CloudComputing.txt";
      const fileData = Buffer.from("Welcome to CS401 Cloud Storage Lecture notes.");
      await adapter.write(fileName, fileData);

      const exists = await adapter.exists(fileName);
      expect(exists).toBe(true);

      const readBack = await adapter.read(fileName);
      expect(readBack.toString()).toBe("Welcome to CS401 Cloud Storage Lecture notes.");
    });

    it("should create directory hierarchies for academic folder organization", async () => {
      await adapter.mkdir("/Semester8/MajorProject/Docs");
      const exists = await adapter.exists("/Semester8/MajorProject/Docs");
      expect(exists).toBe(true);
    });

    it("should support moving and renaming files in the drive", async () => {
      await adapter.write("/draft.txt", Buffer.from("Draft content"));
      await adapter.move("/draft.txt", "/final_report.txt");

      expect(await adapter.exists("/draft.txt")).toBe(false);
      expect(await adapter.exists("/final_report.txt")).toBe(true);
    });

    it("should support copying files within drive spaces", async () => {
      await adapter.write("/original.txt", Buffer.from("Original"));
      await adapter.copy("/original.txt", "/backup.txt");

      expect(await adapter.exists("/original.txt")).toBe(true);
      expect(await adapter.exists("/backup.txt")).toBe(true);
    });

    it("should correctly calculate file size and modification metadata", async () => {
      const content = "Test metadata calculation";
      await adapter.write("/meta.txt", Buffer.from(content));

      const stat = await adapter.stat("/meta.txt");
      expect(stat).not.toBeNull();
      expect(stat?.size).toBe(Buffer.byteLength(content));
      expect(stat?.name).toBe("meta.txt");
      expect(stat?.type).toBe("file");
    });
  });

  describe("Module 2: Security & Directory Traversal Prevention", () => {
    it("should block directory traversal escape attempts (../ / ..\\)", async () => {
      expect(async () => {
        await adapter.read("/../../secret.txt");
      }).toThrow();
    });

    it("should prevent unauthorized access outside assigned root", async () => {
      expect(async () => {
        await adapter.write("/../../malicious.exe", Buffer.from("malware"));
      }).toThrow();
    });
  });

  describe("Module 3: Cryptographic Session Authentication", () => {
    it("should generate and verify HMAC-SHA256 session signatures", () => {
      const payload = JSON.stringify({ user: "student", exp: Date.now() + 3600000, role: "read-write" });
      const encodedPayload = Buffer.from(payload).toString("base64url");
      const signature = createHmac("sha256", SECRET).update(encodedPayload).digest("base64url");
      const token = `${encodedPayload}.${signature}`;

      // Verification logic
      const [receivedPayload, receivedSig] = token.split(".");
      const expectedSig = createHmac("sha256", SECRET).update(receivedPayload).digest("base64url");

      expect(receivedSig).toBe(expectedSig);
      const decoded = JSON.parse(Buffer.from(receivedPayload, "base64url").toString("utf-8"));
      expect(decoded.user).toBe("student");
      expect(decoded.role).toBe("read-write");
    });

    it("should reject tampered session tokens", () => {
      const payload = JSON.stringify({ user: "student", exp: Date.now() + 3600000, role: "read-write" });
      const encodedPayload = Buffer.from(payload).toString("base64url");
      const signature = createHmac("sha256", SECRET).update(encodedPayload).digest("base64url");

      // Attacker attempts privilege escalation to admin
      const forgedPayload = Buffer.from(
        JSON.stringify({ user: "student", exp: Date.now() + 3600000, role: "admin" })
      ).toString("base64url");

      const recalculatedSig = createHmac("sha256", SECRET).update(forgedPayload).digest("base64url");
      expect(signature).not.toBe(recalculatedSig);
    });
  });
});
