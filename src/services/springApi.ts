const TOKEN_KEY = "clouddrive_jwt_token";

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function request(endpoint: string, options: RequestInit = {}) {
  const token = getAuthToken();
  const headers = new Headers(options.headers || {});

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const isFormData =
    typeof FormData !== "undefined" &&
    (options.body instanceof FormData || (options.body && typeof (options.body as any).append === "function"));

  if (isFormData) {
    headers.delete("Content-Type");
  } else if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  // Preserve Cloud Shell authuser query parameter to prevent proxy authentication redirects
  let targetUrl = endpoint;
  if (typeof window !== "undefined" && window.location && window.location.search) {
    try {
      const currentParams = new URLSearchParams(window.location.search);
      const authUser = currentParams.get("authuser");
      if (authUser) {
        const parsed = new URL(endpoint, window.location.origin);
        if (!parsed.searchParams.has("authuser")) {
          parsed.searchParams.set("authuser", authUser);
          targetUrl = parsed.pathname + parsed.search;
        }
      }
    } catch {
      // Fallback to original endpoint if parsing fails
    }
  }

  let response: Response;
  try {
    response = await fetch(targetUrl, {
      ...options,
      credentials: "include",
      headers,
    });
  } catch (err: any) {
    throw new Error(
      "Unable to connect to the backend server. Please verify that the application is running on port 8080 in Cloud Shell."
    );
  }

  if (response.status === 401 || response.status === 403 || (endpoint === "/api/auth/me" && !response.ok)) {
    if (!endpoint.includes("/api/auth/login") && !endpoint.includes("/api/auth/register")) {
      clearAuthToken();
    }
  }

  return response;
}

export async function requestJson<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const response = await request(endpoint, options);
  const text = await response.text();
  const contentType = response.headers.get("content-type") || "";

  if (response.ok && contentType.includes("text/html")) {
    throw new Error("Received HTML response instead of JSON. The backend server proxy may not be connected.");
  }

  let data: any = null;
  if (text && text.trim().length > 0) {
    try {
      data = JSON.parse(text);
    } catch {
      if (response.ok) {
        throw new Error("Invalid JSON response from server.");
      }
      data = { message: text };
    }
  } else {
    data = {};
  }

  if (!response.ok) {
    const errorMsg =
      (data && (data.error || data.message)) ||
      `Request failed with status ${response.status}`;
    throw new Error(errorMsg);
  }
  return data as T;
}

// User & Auth Types
export interface UserProfile {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: string;
  quotaBytes: number;
  usedBytes: number;
  token?: string;
}

export interface QuotaInfo {
  usedBytes: number;
  quotaBytes: number;
  usedPercentage: number;
}

export interface DriveItem {
  id: number;
  name: string;
  path: string;
  directory: boolean;
  size: number;
  contentType: string;
  extension: string;
  starred: boolean;
  trashed: boolean;
  trashedAt?: string;
  color?: string;
  tags?: string;
  versionCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface FileVersionItem {
  id: number;
  versionNumber: number;
  size: number;
  changeSummary: string;
  createdAt: string;
}

export interface UserShareItem {
  id: number;
  fileItem: DriveItem;
  sharedByUsername: string;
  sharedByFullName: string;
  sharedWithUsername: string;
  permission: "VIEW" | "EDIT";
  createdAt: string;
}

export interface ShareInfo {
  id: number;
  token: string;
  fileId: number;
  fileName: string;
  fileSize: number;
  contentType: string;
  isExpired: boolean;
  expiresAt?: string;
  downloadCount: number;
  createdAt: string;
  shareUrl: string;
}

export interface ActivityItem {
  id: number;
  action: string;
  targetName: string;
  details: string;
  timestamp: string;
}

export interface StorageBreakdown {
  documentsBytes: number;
  imagesBytes: number;
  videosBytes: number;
  audioBytes: number;
  codeBytes: number;
  archivesBytes: number;
  otherBytes: number;
  trashBytes: number;
  totalUsedBytes: number;
  totalQuotaBytes: number;
}

// Auth APIs
export async function registerUser(username: string, email: string, password: string, fullName?: string) {
  return requestJson<UserProfile>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password, fullName }),
  });
}

export async function loginUser(username: string, password: string) {
  return requestJson<UserProfile>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export async function fetchCurrentUser() {
  return requestJson<UserProfile>("/api/auth/me");
}

export async function updateUserProfile(fullName: string, email: string) {
  return requestJson<UserProfile>("/api/auth/profile", {
    method: "PUT",
    body: JSON.stringify({ fullName, email }),
  });
}

export async function changeUserPassword(oldPassword: string, newPassword: string) {
  return requestJson<{ message: string }>("/api/auth/change-password", {
    method: "PUT",
    body: JSON.stringify({ oldPassword, newPassword }),
  });
}

// File Operations APIs
export async function fetchFiles(path: string = "/") {
  return requestJson<DriveItem[]>(`/api/files?path=${encodeURIComponent(path)}`);
}

export async function fetchTrash() {
  return requestJson<DriveItem[]>("/api/files/trash");
}

export async function fetchStarred() {
  return requestJson<DriveItem[]>("/api/files/starred");
}

export async function fetchRecent() {
  return requestJson<DriveItem[]>("/api/files/recent");
}

export async function searchFiles(query: string) {
  return requestJson<DriveItem[]>(`/api/files/search?q=${encodeURIComponent(query)}`);
}

export async function fetchQuota() {
  return requestJson<QuotaInfo>("/api/files/quota");
}

export async function fetchStorageBreakdown() {
  return requestJson<StorageBreakdown>("/api/files/storage-breakdown");
}

export async function createFolder(path: string, name: string) {
  return requestJson<DriveItem>("/api/files/folder", {
    method: "POST",
    body: JSON.stringify({ path, name }),
  });
}

export async function createBlankDoc(path: string, name: string, content: string = "") {
  return requestJson<DriveItem>("/api/files/blank", {
    method: "POST",
    body: JSON.stringify({ path, name, content }),
  });
}

export async function uploadFiles(path: string, files: File[]) {
  const formData = new FormData();
  for (const file of files) {
    formData.append("files", file);
  }
  return requestJson<DriveItem[]>(`/api/files/upload?path=${encodeURIComponent(path)}`, {
    method: "POST",
    body: formData,
  });
}

export function getFileDownloadUrl(id: number) {
  const token = getAuthToken();
  return `/api/files/download/${id}${token ? `?token=${encodeURIComponent(token)}` : ""}`;
}

export function getFolderZipDownloadUrl(id: number) {
  const token = getAuthToken();
  return `/api/files/download-folder/${id}${token ? `?token=${encodeURIComponent(token)}` : ""}`;
}

export async function downloadMultipleAsZip(ids: number[]) {
  const res = await request("/api/files/download-zip", {
    method: "POST",
    body: JSON.stringify(ids),
  });
  if (!res.ok) throw new Error("Failed to download zip");
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "drive_files.zip";
  a.click();
  window.URL.revokeObjectURL(url);
}

export function getFilePreviewUrl(id: number) {
  const token = getAuthToken();
  return `/api/files/preview/${id}${token ? `?token=${encodeURIComponent(token)}` : ""}`;
}

export async function fetchFileContent(id: number) {
  return requestJson<{ id: number; name: string; content: string }>(`/api/files/content/${id}`);
}

export async function saveFileContent(id: number, content: string) {
  return requestJson<DriveItem>(`/api/files/content/${id}`, {
    method: "PUT",
    body: JSON.stringify({ content }),
  });
}

export async function fetchVersions(fileId: number) {
  return requestJson<FileVersionItem[]>(`/api/files/versions/${fileId}`);
}

export async function restoreVersion(fileId: number, versionId: number) {
  return requestJson<DriveItem>(`/api/files/versions/${fileId}/restore/${versionId}`, {
    method: "POST",
  });
}

export async function updateFileMeta(id: number, color?: string, tags?: string) {
  return requestJson<DriveItem>(`/api/files/meta/${id}`, {
    method: "PUT",
    body: JSON.stringify({ color, tags }),
  });
}

export async function executeBatchAction(ids: number[], action: string, targetPath?: string) {
  return requestJson<{ message: string }>("/api/files/batch", {
    method: "POST",
    body: JSON.stringify({ ids, action, targetPath }),
  });
}

export async function renameItem(id: number, newName: string) {
  return requestJson<DriveItem>(`/api/files/rename/${id}`, {
    method: "PUT",
    body: JSON.stringify({ newName }),
  });
}

export async function moveItem(id: number, targetPath: string) {
  return requestJson<DriveItem>(`/api/files/move/${id}`, {
    method: "PUT",
    body: JSON.stringify({ targetPath }),
  });
}

export async function toggleStarItem(id: number) {
  return requestJson<DriveItem>(`/api/files/star/${id}`, {
    method: "POST",
  });
}

export async function moveItemToTrash(id: number) {
  return requestJson<{ message: string }>(`/api/files/${id}`, {
    method: "DELETE",
  });
}

export async function restoreItemFromTrash(id: number) {
  return requestJson<{ message: string }>(`/api/files/trash/restore/${id}`, {
    method: "POST",
  });
}

export async function emptyTrashPermanently() {
  return requestJson<{ message: string }>("/api/files/trash/empty", {
    method: "DELETE",
  });
}

// Sharing APIs (Public Links)
export async function createShareLink(fileId: number, expiresInHours?: number) {
  return requestJson<ShareInfo>("/api/share", {
    method: "POST",
    body: JSON.stringify({ fileId, expiresInHours }),
  });
}

export async function fetchMyShares() {
  return requestJson<ShareInfo[]>("/api/share/my-shares");
}

export async function revokeShareLink(shareId: number) {
  return requestJson<{ message: string }>(`/api/share/${shareId}`, {
    method: "DELETE",
  });
}

export async function fetchPublicShareInfo(token: string) {
  return requestJson<ShareInfo>(`/api/share/info/${token}`);
}

export function getPublicDownloadUrl(token: string) {
  return `/api/share/download/${token}`;
}

// Internal User-to-User Sharing APIs
export async function shareWithUser(fileId: number, targetUsername: string, permission: "VIEW" | "EDIT" = "VIEW") {
  return requestJson<UserShareItem>("/api/share/user", {
    method: "POST",
    body: JSON.stringify({ fileId, targetUsername, permission }),
  });
}

export async function fetchSharedWithMe() {
  return requestJson<UserShareItem[]>("/api/share/shared-with-me");
}

export async function fetchSharedByMe() {
  return requestJson<UserShareItem[]>("/api/share/shared-by-me");
}

export async function revokeUserShare(shareId: number) {
  return requestJson<{ message: string }>(`/api/share/user/${shareId}`, {
    method: "DELETE",
  });
}

// Activity Log
export async function fetchActivities() {
  return requestJson<ActivityItem[]>("/api/activity");
}
