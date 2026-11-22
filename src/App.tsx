import React, { useState, useEffect, useRef } from "react";
import {
  Cloud,
  Folder,
  FileText,
  FileCode,
  Image as ImageIcon,
  Film,
  Music,
  Archive,
  Star,
  Trash2,
  Share2,
  Link2,
  Download,
  Eye,
  EyeOff,
  Edit3,
  MoreVertical,
  Plus,
  Upload,
  Search,
  Grid,
  List as ListIcon,
  ChevronRight,
  HardDrive,
  Clock,
  LogOut,
  X,
  Check,
  Copy,
  ExternalLink,
  RefreshCw,
  FolderPlus,
  AlertCircle,
  CheckCircle2,
  ChevronUp,
  FileCheck,
  RotateCcw,
  Users,
  PieChart,
  Settings,
  History,
  Palette,
  ArrowUpDown,
  Lock,
  Play,
  FilePlus,
  FolderDown,
  CheckSquare,
  Square,
  ChevronDown,
  HelpCircle,
  SlidersHorizontal,
  Info
} from "lucide-react";

import {
  fetchCurrentUser,
  clearAuthToken,
  fetchFiles,
  fetchTrash,
  fetchStarred,
  fetchRecent,
  searchFiles,
  fetchQuota,
  createFolder,
  createBlankDoc,
  uploadFiles,
  getFileDownloadUrl,
  getFolderZipDownloadUrl,
  downloadMultipleAsZip,
  getFilePreviewUrl,
  fetchFileContent,
  saveFileContent,
  fetchVersions,
  restoreVersion,
  updateFileMeta,
  executeBatchAction,
  renameItem,
  moveItem,
  toggleStarItem,
  moveItemToTrash,
  restoreItemFromTrash,
  emptyTrashPermanently,
  createShareLink,
  fetchMyShares,
  revokeShareLink,
  shareWithUser,
  fetchSharedWithMe,
  revokeUserShare,
  fetchPublicShareInfo,
  getPublicDownloadUrl,
  fetchStorageBreakdown,
  updateUserProfile,
  changeUserPassword,
  type UserProfile,
  type DriveItem,
  type QuotaInfo,
  type ShareInfo,
  type UserShareItem,
  type StorageBreakdown,
  type FileVersionItem,
} from "./services/springApi";

import { AuthModal } from "./components/AuthModal";
import { useTheme } from "./hooks/useTheme";
import { DriveLogo } from "./components/DriveLogo";
import { ThemeToggle } from "./components/ThemeToggle";



export function App() {
  const [theme, , toggleTheme] = useTheme();

  // Auth State
  const [user, setUser] = useState<UserProfile | null>(null);
  const [authLoading, setAuthLoading] = useState(true);

  // Navigation & View State
  const [currentView, setCurrentView] = useState<
    "drive" | "recent" | "starred" | "shared_with_me" | "shares" | "analytics" | "trash"
  >("drive");
  const [currentPath, setCurrentPath] = useState<string>("/");
  const [viewMode, setViewMode] = useState<"grid" | "list">("grid");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedFilter, setSelectedFilter] = useState<"all" | "document" | "image" | "audio" | "video" | "archive" | "code">("all");
  const [sortBy, setSortBy] = useState<"name" | "date" | "size">("date");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");

  // Selection state for batch operations
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  // Data State
  const [items, setItems] = useState<DriveItem[]>([]);
  const [quota, setQuota] = useState<QuotaInfo | null>(null);
  const [breakdown, setBreakdown] = useState<StorageBreakdown | null>(null);
  const [shares, setShares] = useState<ShareInfo[]>([]);
  const [sharedWithMe, setSharedWithMe] = useState<UserShareItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorAlert, setErrorAlert] = useState<string | null>(null);
  const [successAlert, setSuccessAlert] = useState<string | null>(null);

  // Modals
  const [newMenuOpen, setNewMenuOpen] = useState(false);
  const [newFolderOpen, setNewFolderOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState("");
  const [newDocOpen, setNewDocOpen] = useState(false);
  const [newDocName, setNewDocName] = useState("");
  const [newDocExt, setNewDocExt] = useState(".txt");

  const [renameItemTarget, setRenameItemTarget] = useState<DriveItem | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [moveItemTarget, setMoveItemTarget] = useState<DriveItem | null>(null);
  const [moveTargetPath, setMoveTargetPath] = useState("");

  const [shareItemTarget, setShareItemTarget] = useState<DriveItem | null>(null);
  const [shareTab, setShareTab] = useState<"public" | "user">("public");
  const [shareExpiryHours, setShareExpiryHours] = useState<number>(24);
  const [generatedShare, setGeneratedShare] = useState<ShareInfo | null>(null);
  const [shareTargetUsername, setShareTargetUsername] = useState("");
  const [sharePermission, setSharePermission] = useState<"VIEW" | "EDIT">("VIEW");

  const [versionsTarget, setVersionsTarget] = useState<DriveItem | null>(null);
  const [versionsList, setVersionsList] = useState<FileVersionItem[]>([]);
  const [versionsLoading, setVersionsLoading] = useState(false);

  const [colorPickerTarget, setColorPickerTarget] = useState<DriveItem | null>(null);
  const [activeMenuId, setActiveMenuId] = useState<number | null>(null);

  // Preview Modals
  const [previewMedia, setPreviewMedia] = useState<{ url: string; type: "image" | "audio" | "video" | "pdf"; name: string } | null>(null);

  // Editor Modal
  const [editorItem, setEditorItem] = useState<{ id: number; name: string; content: string } | null>(null);
  const [editorSaving, setEditorSaving] = useState(false);

  // Settings / Profile Modal & Popover
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement | null>(null);
  const [profileName, setProfileName] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);

  // Floating Upload Progress Popup Widget at Right End
  interface UploadQueueItem {
    id: string;
    name: string;
    size: number;
    status: "uploading" | "completed" | "error";
    progress: number;
  }
  const [uploadQueue, setUploadQueue] = useState<UploadQueueItem[]>([]);
  const [uploadPopupOpen, setUploadPopupOpen] = useState(false);
  const [uploadPopupMinimized, setUploadPopupMinimized] = useState(false);

  // Drag and drop & File input
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const folderInputRef = useRef<HTMLInputElement | null>(null);

  // Public Share view
  const [publicShareToken, setPublicShareToken] = useState<string | null>(null);
  const [publicShareInfo, setPublicShareInfo] = useState<ShareInfo | null>(null);

  // 1. Initial Load
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const tokenParam = params.get("share");
    if (tokenParam) {
      setPublicShareToken(tokenParam);
      fetchPublicShareInfo(tokenParam)
        .then(setPublicShareInfo)
        .catch((e) => setErrorAlert(e.message))
        .finally(() => setAuthLoading(false));
      return;
    }

    fetchCurrentUser()
      .then((profile) => {
        setUser(profile);
        setProfileName(profile.fullName || "");
        setProfileEmail(profile.email || "");
      })
      .catch(() => setUser(null))
      .finally(() => setAuthLoading(false));
  }, []);

  // Close Three Dots Menu & Profile Menu when clicking outside
  useEffect(() => {
    const handleDocClick = (e: MouseEvent) => {
      setActiveMenuId(null);
      if (profileMenuRef.current && !profileMenuRef.current.contains(e.target as Node)) {
        setProfileMenuOpen(false);
      }
    };
    window.addEventListener("mousedown", handleDocClick);
    return () => window.removeEventListener("mousedown", handleDocClick);
  }, []);

  // 2. Load View Data
  useEffect(() => {
    if (!user) return;
    loadCurrentViewData();
    loadQuota();
    setSelectedIds(new Set());
  }, [user, currentView, currentPath, searchQuery]);

  const loadQuota = async () => {
    try {
      const q = await fetchQuota();
      setQuota(q);
      const b = await fetchStorageBreakdown();
      setBreakdown(b);
    } catch (e) {}
  };

  const loadCurrentViewData = async () => {
    setLoading(true);
    setErrorAlert(null);
    try {
      if (searchQuery.trim()) {
        const results = await searchFiles(searchQuery.trim());
        setItems(results);
      } else if (currentView === "drive") {
        const list = await fetchFiles(currentPath);
        setItems(list);
      } else if (currentView === "recent") {
        const list = await fetchRecent();
        setItems(list);
      } else if (currentView === "starred") {
        const list = await fetchStarred();
        setItems(list);
      } else if (currentView === "shared_with_me") {
        const list = await fetchSharedWithMe();
        setSharedWithMe(list);
      } else if (currentView === "shares") {
        const list = await fetchMyShares();
        setShares(list);
      } else if (currentView === "analytics") {
        const b = await fetchStorageBreakdown();
        setBreakdown(b);
      } else if (currentView === "trash") {
        const list = await fetchTrash();
        setItems(list);
      }
    } catch (err: any) {
      setErrorAlert(err.message || "Failed to load drive items");
    } finally {
      setLoading(false);
    }
  };

  const notifySuccess = (msg: string) => {
    setSuccessAlert(msg);
    setTimeout(() => setSuccessAlert(null), 4000);
  };

  // Upload Handlers with Floating Popup Tracking
  const handleUploadFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const fileArr = Array.from(files);

    const queueItems = fileArr.map((f, i) => ({
      id: `${Date.now()}-${i}`,
      name: f.name,
      size: f.size,
      status: "uploading" as const,
      progress: 60,
    }));

    setUploadQueue(queueItems);
    setUploadPopupOpen(true);
    setUploadPopupMinimized(false);
    setLoading(true);

    try {
      await uploadFiles(currentPath, fileArr);
      setUploadQueue((prev) =>
        prev.map((item) => ({ ...item, status: "completed" as const, progress: 100 }))
      );
      notifySuccess(`Uploaded ${fileArr.length} file(s) successfully`);
      loadCurrentViewData();
      loadQuota();
    } catch (err: any) {
      setUploadQueue((prev) =>
        prev.map((item) => ({ ...item, status: "error" as const, progress: 0 }))
      );
      setErrorAlert(err.message || "Upload failed");
    } finally {
      setLoading(false);
    }
  };

  // Create Folder
  const handleCreateFolder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;
    try {
      await createFolder(currentPath, newFolderName.trim());
      setNewFolderOpen(false);
      setNewFolderName("");
      notifySuccess("Folder created successfully");
      loadCurrentViewData();
    } catch (err: any) {
      setErrorAlert(err.message || "Failed to create folder");
    }
  };

  // Create Blank Document
  const handleCreateDoc = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDocName.trim()) return;
    try {
      const fullName = newDocName.trim().endsWith(newDocExt)
        ? newDocName.trim()
        : newDocName.trim() + newDocExt;
      const initialText = `# ${newDocName}\n`;
      const doc = await createBlankDoc(currentPath, fullName, initialText);
      setNewDocOpen(false);
      setNewDocName("");
      notifySuccess(`Created document ${doc.name}`);
      loadCurrentViewData();
      loadQuota();
      // Open editor directly
      setEditorItem({ id: doc.id, name: doc.name, content: initialText });
    } catch (err: any) {
      setErrorAlert(err.message || "Failed to create document");
    }
  };

  // Rename
  const handleRename = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!renameItemTarget || !renameValue.trim()) return;
    try {
      await renameItem(renameItemTarget.id, renameValue.trim());
      setRenameItemTarget(null);
      notifySuccess("Renamed successfully");
      loadCurrentViewData();
    } catch (err: any) {
      setErrorAlert(err.message || "Rename failed");
    }
  };

  // Move
  const handleMove = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!moveItemTarget) return;
    try {
      await moveItem(moveItemTarget.id, moveTargetPath.trim() || "/");
      setMoveItemTarget(null);
      notifySuccess(`Moved "${moveItemTarget.name}" to ${moveTargetPath || "/"}`);
      loadCurrentViewData();
    } catch (err: any) {
      setErrorAlert(err.message || "Move failed");
    }
  };

  // Color picker
  const handleSetColor = async (color: string) => {
    if (!colorPickerTarget) return;
    try {
      await updateFileMeta(colorPickerTarget.id, color);
      setColorPickerTarget(null);
      loadCurrentViewData();
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // Version history
  const handleOpenVersions = async (item: DriveItem) => {
    setVersionsTarget(item);
    setVersionsLoading(true);
    try {
      const vers = await fetchVersions(item.id);
      setVersionsList(vers);
    } catch (err: any) {
      setErrorAlert(err.message);
    } finally {
      setVersionsLoading(false);
    }
  };

  const handleRestoreVersion = async (verId: number) => {
    if (!versionsTarget) return;
    try {
      await restoreVersion(versionsTarget.id, verId);
      notifySuccess("Restored previous version");
      setVersionsTarget(null);
      loadCurrentViewData();
      loadQuota();
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // Batch actions
  const handleSelectAll = () => {
    if (selectedIds.size === filteredItems.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredItems.map((i) => i.id)));
    }
  };

  const toggleSelectItem = (id: number) => {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelectedIds(next);
  };

  const handleBatchDownloadZip = async () => {
    if (selectedIds.size === 0) return;
    try {
      await downloadMultipleAsZip(Array.from(selectedIds));
      notifySuccess("Downloaded zip archive");
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  const handleBatchTrash = async () => {
    if (selectedIds.size === 0) return;
    try {
      await executeBatchAction(Array.from(selectedIds), "TRASH");
      notifySuccess(`Moved ${selectedIds.size} items to Trash`);
      setSelectedIds(new Set());
      loadCurrentViewData();
      loadQuota();
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // Quick Public Share handler (under Three Dots option)
  const handleOpenPublicShare = async (item: DriveItem) => {
    setActiveMenuId(null);
    setShareItemTarget(item);
    setShareTab("public");
    try {
      const res = await createShareLink(item.id, shareExpiryHours);
      setGeneratedShare(res);
      const fullUrl = `${window.location.origin}/?share=${res.token}`;
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(fullUrl);
        notifySuccess("Public link generated & copied to clipboard!");
      } else {
        notifySuccess("Public link generated!");
      }
    } catch (err: any) {
      setGeneratedShare(null);
      setErrorAlert(err.message || "Failed to generate public link");
    }
  };

  const handleOpenUserShare = (item: DriveItem) => {
    setActiveMenuId(null);
    setShareItemTarget(item);
    setShareTab("user");
    setGeneratedShare(null);
  };

  // Public Share
  const handleGeneratePublicShare = async () => {
    if (!shareItemTarget) return;
    try {
      const res = await createShareLink(shareItemTarget.id, shareExpiryHours);
      setGeneratedShare(res);
      notifySuccess("Public share link generated");
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // User Share
  const handleShareWithUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shareItemTarget || !shareTargetUsername.trim()) return;
    try {
      await shareWithUser(shareItemTarget.id, shareTargetUsername.trim(), sharePermission);
      notifySuccess(`Shared "${shareItemTarget.name}" with @${shareTargetUsername}`);
      setShareItemTarget(null);
      setShareTargetUsername("");
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // Reusable Three Dots Dropdown Menu Component
  const renderThreeDotsMenu = (item: DriveItem, alignRight = true) => {
    if (activeMenuId !== item.id) return null;

    return (
      <div
        className={`absolute ${alignRight ? "right-0" : "left-0"} top-full mt-1.5 w-56 bg-white dark:bg-[#1e1f20] border border-[#e0e2ec] dark:border-[#3c4043] rounded-2xl shadow-2xl py-1.5 z-50 text-xs font-medium text-[#1f1f1f] dark:text-[#e3e3e3] animate-in fade-in zoom-in-95 duration-100`}
        onClick={(e) => e.stopPropagation()}
      >
        {currentView !== "trash" ? (
          <>
            {/* Public Link - Top Prominent Action under Three Dots */}
            <button
              onClick={() => handleOpenPublicShare(item)}
              className="w-full flex items-center gap-3 px-3.5 py-2.5 text-left hover:bg-[#c2e7ff]/30 dark:hover:bg-[#004a77]/30 text-[#0b57d0] dark:text-[#a8c7fa] font-semibold transition-colors cursor-pointer"
            >
              <Link2 className="w-4 h-4 shrink-0 text-[#0b57d0] dark:text-[#a8c7fa]" />
              <div className="flex flex-col min-w-0">
                <span className="leading-tight">Public link</span>
                <span className="text-[10px] font-normal text-slate-500 dark:text-slate-400 truncate">Generate & copy link</span>
              </div>
            </button>

            <button
              onClick={() => handleOpenUserShare(item)}
              className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer"
            >
              <Share2 className="w-4 h-4 shrink-0 text-slate-500 dark:text-slate-400" />
              <span>Share with user</span>
            </button>

            <div className="my-1 border-t border-[#e0e2ec]/60 dark:border-[#3c4043]/60" />

            <button
              onClick={() => {
                setActiveMenuId(null);
                toggleStarItem(item.id).then(loadCurrentViewData);
              }}
              className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer"
            >
              <Star className={`w-4 h-4 shrink-0 ${item.starred ? "text-amber-500 fill-current" : "text-slate-500 dark:text-slate-400"}`} />
              <span>{item.starred ? "Remove from starred" : "Add to starred"}</span>
            </button>

            <a
              href={item.directory ? getFolderZipDownloadUrl(item.id) : getFileDownloadUrl(item.id)}
              download
              onClick={() => setActiveMenuId(null)}
              className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer text-inherit"
            >
              <Download className="w-4 h-4 shrink-0 text-slate-500 dark:text-slate-400" />
              <span>{item.directory ? "Download as ZIP" : "Download"}</span>
            </a>

            <button
              onClick={() => {
                setActiveMenuId(null);
                setRenameItemTarget(item);
                setRenameValue(item.name);
              }}
              className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer"
            >
              <Edit3 className="w-4 h-4 shrink-0 text-slate-500 dark:text-slate-400" />
              <span>Rename</span>
            </button>

            {!item.directory && (
              <button
                onClick={() => {
                  setActiveMenuId(null);
                  handleOpenVersions(item);
                }}
                className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer"
              >
                <History className="w-4 h-4 shrink-0 text-slate-500 dark:text-slate-400" />
                <span>Version history</span>
              </button>
            )}

            {item.directory && (
              <button
                onClick={() => {
                  setActiveMenuId(null);
                  setColorPickerTarget(item);
                }}
                className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer"
              >
                <Palette className="w-4 h-4 shrink-0 text-slate-500 dark:text-slate-400" />
                <span>Folder color</span>
              </button>
            )}

            <div className="my-1 border-t border-[#e0e2ec]/60 dark:border-[#3c4043]/60" />

            <button
              onClick={() => {
                setActiveMenuId(null);
                moveItemToTrash(item.id).then(() => {
                  notifySuccess(`Moved "${item.name}" to Trash`);
                  loadCurrentViewData();
                  loadQuota();
                });
              }}
              className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-red-500/10 text-red-600 dark:text-red-400 transition-colors cursor-pointer"
            >
              <Trash2 className="w-4 h-4 shrink-0" />
              <span>Move to Trash</span>
            </button>
          </>
        ) : (
          <button
            onClick={() => {
              setActiveMenuId(null);
              restoreItemFromTrash(item.id).then(() => {
                notifySuccess(`Restored "${item.name}"`);
                loadCurrentViewData();
                loadQuota();
              });
            }}
            className="w-full flex items-center gap-3 px-3.5 py-2 text-left hover:bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 transition-colors cursor-pointer"
          >
            <RotateCcw className="w-4 h-4 shrink-0" />
            <span>Restore from Trash</span>
          </button>
        )}
      </div>
    );
  };

  // Editor
  const handleOpenEditor = async (item: DriveItem) => {
    try {
      const data = await fetchFileContent(item.id);
      setEditorItem(data);
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  const handleSaveEditor = async () => {
    if (!editorItem) return;
    setEditorSaving(true);
    try {
      await saveFileContent(editorItem.id, editorItem.content);
      notifySuccess("File saved (new version created)");
      loadCurrentViewData();
      loadQuota();
    } catch (err: any) {
      setErrorAlert(err.message);
    } finally {
      setEditorSaving(false);
    }
  };

  // Profile Update
  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const updated = await updateUserProfile(profileName, profileEmail);
      setUser(updated);
      notifySuccess("Profile updated successfully");
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await changeUserPassword(oldPassword, newPassword);
      setOldPassword("");
      setNewPassword("");
      notifySuccess("Password changed successfully");
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // Trash
  const handleEmptyTrash = async () => {
    if (!window.confirm("Permanently delete all items in Trash? This cannot be undone.")) return;
    try {
      await emptyTrashPermanently();
      notifySuccess("Trash emptied permanently");
      loadCurrentViewData();
      loadQuota();
    } catch (err: any) {
      setErrorAlert(err.message);
    }
  };

  // Breadcrumbs
  const navigateToFolder = (path: string) => {
    setCurrentPath(path);
    setCurrentView("drive");
  };

  const breadcrumbs = () => {
    if (currentPath === "/") return [{ name: "My Drive", path: "/" }];
    const parts = currentPath.split("/").filter(Boolean);
    const crumbs = [{ name: "My Drive", path: "/" }];
    let accum = "";
    for (const p of parts) {
      accum += "/" + p;
      crumbs.push({ name: p, path: accum });
    }
    return crumbs;
  };

  // Filtering & Sorting
  const filteredItems = (Array.isArray(items) ? items : [])
    .filter((item) => {
      if (selectedFilter === "all") return true;
      const ext = item.extension.toLowerCase();
      if (selectedFilter === "document") {
        return ["pdf", "doc", "docx", "txt", "md", "csv", "xlsx", "pptx", "rtf"].includes(ext);
      }
      if (selectedFilter === "image") {
        return ["png", "jpg", "jpeg", "gif", "webp", "svg", "bmp"].includes(ext);
      }
      if (selectedFilter === "audio") {
        return ["mp3", "wav", "flac", "aac", "ogg", "m4a"].includes(ext);
      }
      if (selectedFilter === "video") {
        return ["mp4", "mov", "mkv", "webm", "avi"].includes(ext);
      }
      if (selectedFilter === "archive") {
        return ["zip", "rar", "7z", "tar", "gz"].includes(ext);
      }
      if (selectedFilter === "code") {
        return ["js", "ts", "html", "css", "py", "java", "json", "xml", "sql", "sh", "rs", "cpp"].includes(ext);
      }
      return true;
    })
    .sort((a, b) => {
      // Directories always first
      if (a.directory && !b.directory) return -1;
      if (!a.directory && b.directory) return 1;

      let compare = 0;
      if (sortBy === "name") {
        compare = a.name.localeCompare(b.name);
      } else if (sortBy === "size") {
        compare = a.size - b.size;
      } else {
        compare = new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime();
      }
      return sortOrder === "asc" ? compare : -compare;
    });

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
  };

  const getFileIcon = (item: DriveItem) => {
    if (item.directory) {
      const colorStyle = item.color ? { color: item.color } : { color: "#5f6368" };
      return <Folder className="w-5 h-5 fill-current/25" style={colorStyle} />;
    }
    const ext = item.extension.toLowerCase();
    if (["png", "jpg", "jpeg", "gif", "webp", "svg"].includes(ext)) {
      return <ImageIcon className="w-5 h-5 text-red-500" />;
    }
    if (["mp4", "mov", "mkv", "webm"].includes(ext)) {
      return <Film className="w-5 h-5 text-rose-500" />;
    }
    if (["mp3", "wav", "flac", "ogg"].includes(ext)) {
      return <Music className="w-5 h-5 text-purple-500" />;
    }
    if (["zip", "rar", "7z", "tar", "gz"].includes(ext)) {
      return <Archive className="w-5 h-5 text-amber-600" />;
    }
    if (["csv", "xlsx", "xls"].includes(ext)) {
      return <FileText className="w-5 h-5 text-emerald-600" />;
    }
    if (["pptx", "ppt"].includes(ext)) {
      return <FileText className="w-5 h-5 text-amber-500" />;
    }
    if (ext === "pdf") {
      return <FileText className="w-5 h-5 text-red-600" />;
    }
    if (["js", "ts", "html", "css", "py", "java", "json", "xml", "sql", "sh"].includes(ext)) {
      return <FileCode className="w-5 h-5 text-sky-600" />;
    }
    return <FileText className="w-5 h-5 text-[#0b57d0]" />;
  };

  // Preview click handler
  const handleItemPreviewOrOpen = (item: DriveItem) => {
    if (item.directory) {
      navigateToFolder(item.path === "/" ? `/${item.name}` : `${item.path}/${item.name}`);
      return;
    }
    const ext = item.extension.toLowerCase();
    const url = getFilePreviewUrl(item.id);
    if (["png", "jpg", "jpeg", "webp", "gif", "svg"].includes(ext)) {
      setPreviewMedia({ url, type: "image", name: item.name });
    } else if (["mp4", "webm", "mov"].includes(ext)) {
      setPreviewMedia({ url, type: "video", name: item.name });
    } else if (["mp3", "wav", "ogg", "flac"].includes(ext)) {
      setPreviewMedia({ url, type: "audio", name: item.name });
    } else if (ext === "pdf") {
      setPreviewMedia({ url, type: "pdf", name: item.name });
    } else if (["txt", "md", "js", "ts", "html", "css", "py", "java", "json", "xml", "sql", "sh"].includes(ext)) {
      handleOpenEditor(item);
    } else {
      window.open(getFileDownloadUrl(item.id), "_blank");
    }
  };

  // Public Share view
  if (publicShareToken && publicShareInfo) {
    return (
      <div className="min-h-screen theme-bg-app flex flex-col items-center justify-center p-4 transition-colors">
        <div className="w-full max-w-md theme-bg-surface border theme-border rounded-3xl p-8 text-center shadow-xl">
          <div className="flex justify-center mb-4">
            <DriveLogo className="w-12 h-12" />
          </div>
          <h1 className="text-xl font-medium theme-text-main mb-6">Cloud Drive</h1>

          <div className="theme-bg-input p-4 rounded-2xl border theme-border-subtle text-left mb-6">
            <p className="text-xs theme-text-muted mb-1">File</p>
            <p className="text-sm font-medium theme-text-main truncate">{publicShareInfo.fileName}</p>
            <div className="flex justify-between items-center mt-3 pt-3 border-t theme-border-subtle text-xs theme-text-muted">
              <span>{formatBytes(publicShareInfo.fileSize)}</span>
              <span>{publicShareInfo.downloadCount} downloads</span>
            </div>
          </div>

          <a
            href={getPublicDownloadUrl(publicShareToken)}
            download
            className="w-full py-2.5 theme-btn-primary font-medium rounded-full flex items-center justify-center gap-2 transition-all cursor-pointer text-sm shadow-sm hover:shadow"
          >
            <Download className="w-4 h-4" />
            <span>Download</span>
          </a>
        </div>
      </div>
    );
  }

  if (authLoading) {
    return (
      <div className="min-h-screen theme-bg-app flex flex-col items-center justify-center transition-colors duration-300">
        <div className="flex flex-col items-center animate-in fade-in zoom-in-95 duration-500">
          <div className="relative mb-6">
            <div className="absolute inset-0 bg-blue-500/20 dark:bg-blue-400/20 blur-2xl rounded-full scale-150 animate-pulse" />
            <DriveLogo className="w-20 h-20 relative z-10 animate-drive-float" />
          </div>
          <h2 className="text-2xl font-normal theme-text-main tracking-tight font-sans mb-3">
            Cloud Drive
          </h2>
          <div className="w-48 h-1 bg-[var(--app-surface-subtle)] rounded-full overflow-hidden relative border theme-border-subtle">
            <div className="absolute top-0 bottom-0 left-0 bg-[#0b57d0] dark:bg-[#a8c7fa] rounded-full animate-drive-loader" />
          </div>
          <p className="text-xs theme-text-muted mt-3 font-medium animate-pulse">
            Loading your workspace...
          </p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <AuthModal onSuccess={(profile) => setUser(profile)} />;
  }

  return (
    <div
      className="min-h-screen theme-bg-app theme-text-main flex flex-col font-sans select-none transition-colors duration-200"
      onDragOver={(e) => {
        e.preventDefault();
        setIsDragging(true);
      }}
      onDragLeave={() => setIsDragging(false)}
      onDrop={(e) => {
        e.preventDefault();
        setIsDragging(false);
        handleUploadFiles(e.dataTransfer.files);
      }}
    >
      {/* Drag & Drop Overlay */}
      {isDragging && (
        <div className="fixed inset-0 bg-[var(--app-primary-soft)] backdrop-blur-sm border-4 border-dashed border-[var(--app-primary)] z-50 flex flex-col items-center justify-center pointer-events-none">
          <Upload className="w-14 h-14 text-[var(--app-primary-light)] animate-bounce mb-3" />
          <h2 className="text-xl font-medium theme-text-main">Drop files to upload</h2>
        </div>
      )}

      {/* Hidden File and Folder Inputs */}
      <input
        type="file"
        multiple
        ref={fileInputRef}
        className="hidden"
        onChange={(e) => handleUploadFiles(e.target.files)}
      />
      <input
        type="file"
        ref={folderInputRef}
        className="hidden"
        {...({ webkitdirectory: "", directory: "" } as any)}
        onChange={(e) => handleUploadFiles(e.target.files)}
      />

      {/* Top Header */}
      <header className="h-16 px-4 sm:px-6 flex items-center justify-between sticky top-0 z-30 transition-colors select-none theme-bg-app">
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={() => {
              setCurrentView("drive");
              setCurrentPath("/");
              setSearchQuery("");
            }}
            className="flex items-center gap-2.5 cursor-pointer hover:opacity-90 transition-opacity"
          >
            <DriveLogo className="w-9 h-9" />
            <span className="font-medium text-xl tracking-tight theme-text-main font-sans">
              Cloud Drive
            </span>
          </button>
        </div>

        {/* Global Search */}
        <div className="flex-1 max-w-2xl mx-4 sm:mx-8">
          <div className="relative flex items-center w-full h-12 rounded-full bg-[#edf2fc] dark:bg-[#282a2c] hover:bg-[#e9eef6] dark:hover:bg-[#333537] focus-within:bg-white dark:focus-within:bg-[#1e1f20] focus-within:shadow-md border border-transparent focus-within:border-[var(--app-border-focus)] transition-all px-4">
            <Search className="w-5 h-5 text-[var(--app-text-muted)] shrink-0 mr-3" />
            <input
              type="text"
              placeholder="Search in Cloud Drive"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-transparent text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10 theme-text-muted hover:theme-text-main transition-colors ml-1"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* User Profile, Theme Switcher & Actions */}
        <div className="flex items-center gap-1 sm:gap-2 shrink-0">
          <ThemeToggle theme={theme} onToggle={toggleTheme} />

          <button
            onClick={() => setSettingsOpen(true)}
            title="Settings"
            className="p-2.5 rounded-full hover:bg-black/5 dark:hover:bg-white/10 theme-text-sub hover:theme-text-main transition-colors cursor-pointer"
          >
            <Settings className="w-5 h-5" />
          </button>



          <div className="relative pl-1 sm:pl-2" ref={profileMenuRef}>
            <button
              onClick={() => setProfileMenuOpen(!profileMenuOpen)}
              className="w-9 h-9 rounded-full bg-[#0b57d0] hover:bg-[#0842a0] text-white flex items-center justify-center font-medium text-sm shadow-xs hover:ring-2 hover:ring-[var(--app-primary-soft)] transition-all cursor-pointer select-none"
              title={`${user?.fullName || user?.username || "User"} (@${user?.username || "user"})`}
              aria-label="Account options"
            >
              {((user?.fullName || user?.username || "U").charAt(0) || "U").toUpperCase()}
            </button>

            {profileMenuOpen && (
              <div className="absolute right-0 top-12 w-80 theme-bg-surface border theme-border rounded-3xl shadow-2xl p-5 z-50 animate-in fade-in zoom-in-95 duration-150">
                {/* Account Header */}
                <div className="flex flex-col items-center text-center pb-4 border-b theme-border-subtle">
                  <div className="w-16 h-16 rounded-full bg-[#0b57d0] text-white flex items-center justify-center text-2xl font-semibold mb-3 shadow-md ring-4 ring-blue-500/10 select-none">
                    {((user?.fullName || user?.username || "U").charAt(0) || "U").toUpperCase()}
                  </div>
                  <h4 className="text-base font-semibold theme-text-main leading-snug">
                    {user?.fullName || user?.username || "User"}
                  </h4>
                  <p className="text-xs theme-text-muted mt-0.5 font-mono">
                    @{user?.username || "user"}
                  </p>
                  {user?.email && (
                    <p className="text-xs theme-text-sub mt-1">
                      {user.email}
                    </p>
                  )}
                </div>

                {/* Storage Quota preview */}
                <div className="py-3.5 border-b theme-border-subtle">
                  <div className="flex items-center justify-between text-xs theme-text-sub mb-1.5">
                    <span className="flex items-center gap-1.5 font-medium">
                      <Cloud className="w-3.5 h-3.5 theme-text-muted" />
                      Storage
                    </span>
                    <span className="font-semibold theme-text-main">
                      {formatBytes(quota?.usedBytes || 0)} / {formatBytes(quota?.quotaBytes || 524288000)}
                    </span>
                  </div>
                  <div className="w-full bg-[#e0e2ec] dark:bg-[#3c4043] rounded-full h-1.5 overflow-hidden">
                    <div
                      className="bg-[#0b57d0] dark:bg-[#a8c7fa] h-full rounded-full transition-all duration-500"
                      style={{
                        width: `${Math.min(
                          100,
                          Math.round(((quota?.usedBytes || 0) / (quota?.quotaBytes || 1)) * 100)
                        )}%`,
                      }}
                    />
                  </div>
                </div>

                {/* Actions */}
                <div className="pt-3 space-y-1.5">
                  <button
                    onClick={() => {
                      setProfileMenuOpen(false);
                      setSettingsOpen(true);
                    }}
                    className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold theme-text-main hover:theme-bg-hover transition-colors cursor-pointer"
                  >
                    <Settings className="w-4 h-4 theme-text-muted" />
                    <span>Account Settings</span>
                  </button>

                  <button
                    onClick={() => {
                      setProfileMenuOpen(false);
                      clearAuthToken();
                      setUser(null);
                    }}
                    className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold text-red-500 hover:bg-red-500/10 transition-colors cursor-pointer"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Sign Out</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Sidebar */}
        <aside className="w-64 theme-bg-app px-4 pb-4 flex flex-col justify-between shrink-0 transition-colors">
          <div className="space-y-4">
            {/* "+ New" Action Menu */}
            <div className="relative pt-2">
              <button
                onClick={() => setNewMenuOpen(!newMenuOpen)}
                className="gdrive-new-btn h-14 px-6 rounded-2xl flex items-center gap-3 font-medium text-sm transition-all cursor-pointer shadow-md"
              >
                <div className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-xs">
                  <Plus className="w-4 h-4 stroke-[2.5]" />
                </div>
                <span className="font-semibold text-sm tracking-wide">New</span>
              </button>

              {newMenuOpen && (
                <div className="absolute top-18 left-0 w-60 theme-bg-surface border theme-border rounded-2xl shadow-xl p-2 z-40 space-y-1 animate-fadeUp">
                  <button
                    onClick={() => {
                      setNewMenuOpen(false);
                      setNewFolderOpen(true);
                    }}
                    className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold theme-text-sub hover:theme-text-main theme-bg-hover transition-colors"
                  >
                    <FolderPlus className="w-4 h-4 text-amber-500" />
                    <span>New folder</span>
                  </button>
                  <div className="border-t theme-border-subtle my-1" />
                  <button
                    onClick={() => {
                      setNewMenuOpen(false);
                      fileInputRef.current?.click();
                    }}
                    className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold theme-text-sub hover:theme-text-main theme-bg-hover transition-colors"
                  >
                    <Upload className="w-4 h-4 text-blue-500" />
                    <span>File upload</span>
                  </button>
                  <button
                    onClick={() => {
                      setNewMenuOpen(false);
                      folderInputRef.current?.click();
                    }}
                    className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold theme-text-sub hover:theme-text-main theme-bg-hover transition-colors"
                  >
                    <Folder className="w-4 h-4 text-emerald-500" />
                    <span>Folder upload</span>
                  </button>
                  <div className="border-t theme-border-subtle my-1" />
                  <button
                    onClick={() => {
                      setNewMenuOpen(false);
                      setNewDocOpen(true);
                    }}
                    className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold theme-text-sub hover:theme-text-main theme-bg-hover transition-colors"
                  >
                    <FilePlus className="w-4 h-4 text-blue-600" />
                    <span>New document</span>
                  </button>
                </div>
              )}
            </div>

            {/* Navigation Menu */}
            <nav className="space-y-1">
              <button
                onClick={() => {
                  setCurrentView("drive");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "drive"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <HardDrive className="w-4 h-4" />
                <span>My Drive</span>
              </button>

              <button
                onClick={() => {
                  setCurrentView("shared_with_me");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "shared_with_me"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <Users className="w-4 h-4" />
                <span>Shared with me</span>
              </button>

              <button
                onClick={() => {
                  setCurrentView("recent");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "recent"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <Clock className="w-4 h-4" />
                <span>Recent</span>
              </button>

              <button
                onClick={() => {
                  setCurrentView("starred");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "starred"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <Star className="w-4 h-4" />
                <span>Starred</span>
              </button>

              <button
                onClick={() => {
                  setCurrentView("shares");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "shares"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <Share2 className="w-4 h-4" />
                <span>Public links</span>
              </button>

              <button
                onClick={() => {
                  setCurrentView("trash");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "trash"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <Trash2 className="w-4 h-4" />
                <span>Trash</span>
              </button>

              <div className="border-t theme-border-subtle my-2" />

              <button
                onClick={() => {
                  setCurrentView("analytics");
                  setSearchQuery("");
                }}
                className={`w-full flex items-center gap-4 px-4 py-2.5 rounded-full text-sm font-medium transition-colors ${
                  currentView === "analytics"
                    ? "theme-pill-active font-semibold"
                    : "theme-text-sub hover:theme-text-main hover:bg-[#eaebef] dark:hover:bg-[#282a2c]"
                }`}
              >
                <PieChart className="w-4 h-4" />
                <span>Storage</span>
              </button>
            </nav>
          </div>

          {/* Storage Quota Card */}
          {quota && (
            <div className="px-3 py-4">
              <div className="flex items-center gap-2 mb-2">
                <Cloud className="w-4 h-4 theme-text-sub" />
                <span className="text-sm font-medium theme-text-main">Storage</span>
              </div>
              <div className="w-full bg-[#e0e2ec] dark:bg-[#3c4043] rounded-full h-1 overflow-hidden mb-2">
                <div
                  className="h-full bg-[#0b57d0] dark:bg-[#a8c7fa] rounded-full transition-all"
                  style={{ width: `${Math.min(quota.usedPercentage, 100)}%` }}
                />
              </div>
              <p className="text-xs theme-text-muted mb-2">
                {formatBytes(quota.usedBytes)} of {formatBytes(quota.quotaBytes)} used
              </p>
              <button
                onClick={() => setCurrentView("analytics")}
                className="text-xs font-semibold text-[#0b57d0] dark:text-[#a8c7fa] hover:underline cursor-pointer"
              >
                Storage details
              </button>
            </div>
          )}
        </aside>

        {/* Right Content Sheet Canvas */}
        <div className="flex-1 p-2 sm:p-4 pr-3 sm:pr-5 pb-3 sm:pb-5 flex flex-col overflow-hidden">
          <main className="flex-1 flex flex-col overflow-y-auto bg-white dark:bg-[#1e1f20] rounded-2xl border border-[#e0e2ec]/70 dark:border-[#3c4043] p-5 shadow-xs transition-colors">
          {/* Notifications */}
          {errorAlert && (
            <div className="mb-4 p-3 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4" />
                <span>{errorAlert}</span>
              </div>
              <button onClick={() => setErrorAlert(null)}>
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {successAlert && (
            <div className="mb-4 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400 text-sm flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FileCheck className="w-4 h-4" />
                <span>{successAlert}</span>
              </div>
              <button onClick={() => setSuccessAlert(null)}>
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Top Toolbar */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-4">
            {/* Breadcrumb Path */}
            <div className="flex items-center gap-2 text-base font-medium">
              {currentView === "drive" && (
                currentPath === "/" ? (
                  <div className="flex items-center gap-1.5">
                    <span className="text-xl font-normal theme-text-main font-sans">My Drive</span>
                    <ChevronDown className="w-4 h-4 theme-text-muted" />
                  </div>
                ) : (
                  <div className="flex items-center gap-1.5 text-sm font-medium">
                    {breadcrumbs().map((crumb, idx) => (
                      <React.Fragment key={crumb.path}>
                        {idx > 0 && <ChevronRight className="w-4 h-4 theme-text-faint" />}
                        <button
                          onClick={() => navigateToFolder(crumb.path)}
                          className={`hover:text-[#0b57d0] dark:hover:text-[#a8c7fa] transition-colors ${
                            idx === breadcrumbs().length - 1 ? "theme-text-main font-semibold text-base" : "theme-text-muted"
                          }`}
                        >
                          {crumb.name}
                        </button>
                      </React.Fragment>
                    ))}
                  </div>
                )
              )}

              {currentView === "recent" && <span className="text-xl font-normal theme-text-main font-sans">Recent</span>}
              {currentView === "starred" && <span className="text-xl font-normal theme-text-main font-sans">Starred</span>}
              {currentView === "shared_with_me" && <span className="text-xl font-normal theme-text-main font-sans">Shared with me</span>}
              {currentView === "shares" && <span className="text-xl font-normal theme-text-main font-sans">Public links</span>}
              {currentView === "analytics" && <span className="text-xl font-normal theme-text-main font-sans">Storage</span>}
              {currentView === "trash" && <span className="text-xl font-normal theme-text-main font-sans">Trash</span>}
            </div>

            {/* Actions Bar & Batch Operations */}
            <div className="flex items-center gap-2">
              {/* Batch Actions Bar (when items are selected) */}
              {selectedIds.size > 0 && (
                <div className="flex items-center gap-1.5 bg-[#c2e7ff] text-[#001d35] dark:bg-[#004a77] dark:text-[#c2e7ff] px-3.5 py-1.5 rounded-full shadow-sm text-xs font-semibold">
                  <span className="mr-1">{selectedIds.size} selected</span>
                  {selectedIds.size === 1 && (
                    <button
                      onClick={() => {
                        const selectedId = Array.from(selectedIds)[0];
                        const item = items.find((i) => i.id === selectedId);
                        if (item) handleOpenPublicShare(item);
                      }}
                      className="p-1 rounded-full hover:bg-black/10 dark:hover:bg-white/10"
                      title="Get public link"
                    >
                      <Link2 className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={handleBatchDownloadZip}
                    className="p-1 rounded-full hover:bg-black/10 dark:hover:bg-white/10"
                    title="Download Selected as ZIP"
                  >
                    <FolderDown className="w-4 h-4" />
                  </button>
                  <button
                    onClick={handleBatchTrash}
                    className="p-1 rounded-full hover:bg-red-500/20 text-red-600 dark:text-red-300"
                    title="Move Selected to Trash"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                  <button onClick={() => setSelectedIds(new Set())} className="p-1 rounded-full hover:bg-black/10 dark:hover:bg-white/10">
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {/* Sort Selector */}
              {["drive", "recent", "starred"].includes(currentView) && (
                <div className="flex items-center gap-1 bg-[#f0f4f9] dark:bg-[#282a2c] border border-[#e0e2ec] dark:border-[#3c4043] rounded-full px-3 py-1.5 text-xs theme-text-sub">
                  <ArrowUpDown className="w-3.5 h-3.5 theme-text-muted" />
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value as any)}
                    className="bg-transparent theme-text-sub focus:outline-none cursor-pointer text-xs"
                  >
                    <option value="date" className="bg-white dark:bg-[#1e1f20]">Last Modified</option>
                    <option value="name" className="bg-white dark:bg-[#1e1f20]">Name</option>
                    <option value="size" className="bg-white dark:bg-[#1e1f20]">Size</option>
                  </select>
                  <button
                    onClick={() => setSortOrder(sortOrder === "asc" ? "desc" : "asc")}
                    className="ml-1 text-xs font-bold text-[#0b57d0] dark:text-[#a8c7fa] hover:opacity-80"
                  >
                    {sortOrder === "asc" ? "↑" : "↓"}
                  </button>
                </div>
              )}

              {currentView === "trash" && items.length > 0 && (
                <button
                  onClick={handleEmptyTrash}
                  className="py-1.5 px-4 bg-red-600 hover:bg-red-700 text-white text-xs font-semibold rounded-full flex items-center gap-1.5 transition-colors cursor-pointer shadow-xs"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Empty trash</span>
                </button>
              )}

              {/* View Toggle */}
              <div className="flex bg-[#f0f4f9] dark:bg-[#282a2c] border border-[#e0e2ec] dark:border-[#3c4043] rounded-full p-0.5">
                <button
                  onClick={() => setViewMode("grid")}
                  title="Grid view"
                  className={`p-1.5 rounded-full transition-colors ${viewMode === "grid" ? "bg-white dark:bg-[#1e1f20] text-[#0b57d0] dark:text-[#a8c7fa] shadow-xs" : "theme-text-muted hover:theme-text-main"}`}
                >
                  <Grid className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setViewMode("list")}
                  title="List view"
                  className={`p-1.5 rounded-full transition-colors ${viewMode === "list" ? "bg-white dark:bg-[#1e1f20] text-[#0b57d0] dark:text-[#a8c7fa] shadow-xs" : "theme-text-muted hover:theme-text-main"}`}
                >
                  <ListIcon className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* Filter Pills */}
          {["drive", "starred", "recent"].includes(currentView) && (
            <div className="flex items-center gap-2 mb-5 overflow-x-auto pb-1">
              {(["all", "document", "image", "audio", "video", "archive", "code"] as const).map((filter) => (
                <button
                  key={filter}
                  onClick={() => setSelectedFilter(filter)}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-medium capitalize transition-all border ${
                    selectedFilter === filter
                      ? "bg-[#c2e7ff] text-[#001d35] border-[#0b57d0]/40 font-semibold dark:bg-[#004a77] dark:text-[#c2e7ff]"
                      : "bg-white dark:bg-[#1e1f20] theme-text-sub border-[#e0e2ec] dark:border-[#3c4043] hover:bg-[#f0f4f9] dark:hover:bg-[#282a2c]"
                  }`}
                >
                  {filter === "all" ? "Type" : filter}
                </button>
              ))}
            </div>
          )}

          {/* VIEW: Storage Breakdown */}
          {currentView === "analytics" && breakdown && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="theme-bg-surface border theme-border rounded-2xl p-6 shadow-sm">
                  <p className="text-xs uppercase theme-text-muted font-semibold mb-1">Total storage</p>
                  <p className="text-2xl font-bold theme-text-main">{formatBytes(breakdown.totalQuotaBytes)}</p>
                </div>
                <div className="theme-bg-surface border theme-border rounded-2xl p-6 shadow-sm">
                  <p className="text-xs uppercase theme-text-muted font-semibold mb-1">Used storage</p>
                  <p className="text-2xl font-bold text-[var(--app-primary-light)]">{formatBytes(breakdown.totalUsedBytes)}</p>
                </div>
                <div className="theme-bg-surface border theme-border rounded-2xl p-6 shadow-sm">
                  <p className="text-xs uppercase theme-text-muted font-semibold mb-1">Trash</p>
                  <p className="text-2xl font-bold text-red-400">{formatBytes(breakdown.trashBytes)}</p>
                </div>
              </div>

              {/* Category Breakdown Bars */}
              <div className="theme-bg-surface border theme-border rounded-2xl p-6 shadow-sm">
                <h3 className="text-base font-bold theme-text-main mb-4">Storage details</h3>
                <div className="space-y-4">
                  {[
                    { label: "Documents", size: breakdown.documentsBytes, color: "bg-blue-500" },
                    { label: "Images", size: breakdown.imagesBytes, color: "bg-emerald-500" },
                    { label: "Videos", size: breakdown.videosBytes, color: "bg-rose-500" },
                    { label: "Audio", size: breakdown.audioBytes, color: "bg-purple-500" },
                    { label: "Code Files", size: breakdown.codeBytes, color: "bg-sky-500" },
                    { label: "Archives", size: breakdown.archivesBytes, color: "bg-orange-500" },
                    { label: "Other Files", size: breakdown.otherBytes, color: "bg-slate-500" },
                  ].map((cat) => (
                    <div key={cat.label}>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-slate-300 font-semibold">{cat.label}</span>
                        <span className="text-slate-400 font-mono">{formatBytes(cat.size)}</span>
                      </div>
                      <div className="w-full theme-bg-input rounded-full h-2 overflow-hidden border theme-border-subtle">
                        <div
                          className={`h-full ${cat.color} rounded-full`}
                          style={{
                            width: `${Math.min(
                              (cat.size / Math.max(breakdown.totalUsedBytes, 1)) * 100,
                              100
                            )}%`,
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* VIEW: Shared with Me */}
          {currentView === "shared_with_me" && (
            <div className="theme-bg-surface border theme-border rounded-2xl overflow-hidden shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="theme-bg-subtle border-b theme-border text-xs uppercase theme-text-muted font-semibold">
                  <tr>
                    <th className="py-3 px-4">File Name</th>
                    <th className="py-3 px-4">Shared By</th>
                    <th className="py-3 px-4">Permission</th>
                    <th className="py-3 px-4">Date Shared</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y theme-border-subtle">
                  {(Array.isArray(sharedWithMe) ? sharedWithMe : []).map((share) => (
                    <tr key={share.id} className="hover:theme-bg-hover transition-colors">
                      <td className="py-3 px-4 font-semibold theme-text-main flex items-center gap-2">
                        {getFileIcon(share.fileItem)}
                        <span>{share.fileItem.name}</span>
                      </td>
                      <td className="py-3 px-4 text-[var(--app-primary-light)] font-mono text-xs">@{share.sharedByUsername}</td>
                      <td className="py-3 px-4 text-xs">
                        <span className="px-2 py-0.5 rounded theme-bg-subtle border theme-border theme-text-sub">
                          {share.permission}
                        </span>
                      </td>
                      <td className="py-3 px-4 theme-text-muted text-xs">{new Date(share.createdAt).toLocaleDateString()}</td>
                      <td className="py-3 px-4 text-right space-x-2">
                        <a
                          href={`/api/share/user-download/${share.id}`}
                          download
                          className="p-1.5 rounded-lg theme-btn-primary inline-block"
                        >
                          <Download className="w-4 h-4" />
                        </a>
                      </td>
                    </tr>
                  ))}
                  {sharedWithMe.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-8 text-center theme-text-muted text-sm">
                        No shared files
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* VIEW: Public Shares */}
          {currentView === "shares" && (
            <div className="theme-bg-surface border theme-border rounded-2xl overflow-hidden shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="theme-bg-subtle border-b theme-border text-xs uppercase theme-text-muted font-semibold">
                  <tr>
                    <th className="py-3 px-4">File Name</th>
                    <th className="py-3 px-4">Size</th>
                    <th className="py-3 px-4">Expires</th>
                    <th className="py-3 px-4">Downloads</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y theme-border-subtle">
                  {(Array.isArray(shares) ? shares : []).map((share) => (
                    <tr key={share.id} className="hover:theme-bg-hover transition-colors">
                      <td className="py-3 px-4 font-semibold theme-text-main">{share.fileName}</td>
                      <td className="py-3 px-4 theme-text-muted text-xs">{formatBytes(share.fileSize)}</td>
                      <td className="py-3 px-4 text-xs theme-text-sub">
                        {share.expiresAt ? new Date(share.expiresAt).toLocaleDateString() : "Never"}
                      </td>
                      <td className="py-3 px-4 text-xs font-mono text-emerald-400">{share.downloadCount}</td>
                      <td className="py-3 px-4 text-right space-x-2">
                        <button
                          onClick={() => {
                            const fullUrl = `${window.location.origin}/?share=${share.token}`;
                            navigator.clipboard.writeText(fullUrl);
                            notifySuccess("Share link copied to clipboard!");
                          }}
                          className="p-1.5 rounded-lg theme-bg-subtle hover:theme-bg-hover theme-text-sub"
                          title="Copy Link"
                        >
                          <Copy className="w-4 h-4" />
                        </button>
                        <button
                          onClick={async () => {
                            await revokeShareLink(share.id);
                            notifySuccess("Share link revoked");
                            loadCurrentViewData();
                          }}
                          className="p-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/30 text-red-400"
                          title="Revoke Link"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {shares.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-8 text-center theme-text-muted text-sm">
                        No shared links
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* VIEW: Files (Grid & List) */}
          {["drive", "recent", "starred", "trash"].includes(currentView) && (
            <>
              {loading ? (
                <div className="flex-1 flex items-center justify-center py-16">
                  <RefreshCw className="w-8 h-8 text-[var(--app-primary-light)] animate-spin" />
                </div>
              ) : filteredItems.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center py-16 text-center">
                  <div className="w-16 h-16 rounded-2xl theme-bg-surface border theme-border flex items-center justify-center mb-3">
                    <Cloud className="w-8 h-8 theme-text-muted" />
                  </div>
                  <h3 className="text-base font-medium theme-text-muted">
                    {currentView === "trash" ? "Trash is empty" : "No files"}
                  </h3>
                </div>
              ) : viewMode === "grid" ? (
                /* Grid View */
                <div className="space-y-6">
                  {/* Folders Section */}
                  {filteredItems.filter((i) => i.directory).length > 0 && (
                    <div>
                      <h3 className="text-sm font-medium theme-text-sub mb-3">Folders</h3>
                      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                        {filteredItems
                          .filter((i) => i.directory)
                          .map((item) => (
                            <div
                              key={item.id}
                              className={`group bg-[#f0f4f9] hover:bg-[#e1e9f5] dark:bg-[#282a2c] dark:hover:bg-[#333537] rounded-xl px-3.5 py-3 flex items-center justify-between border ${
                                selectedIds.has(item.id)
                                  ? "border-[#0b57d0] dark:border-[#a8c7fa] bg-[#c2e7ff]/30"
                                  : "border-transparent hover:border-[#c2e7ff]/70"
                              } transition-all cursor-pointer shadow-xs`}
                              onClick={() => handleItemPreviewOrOpen(item)}
                            >
                              <div className="flex items-center gap-3 min-w-0 pr-2">
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    toggleSelectItem(item.id);
                                  }}
                                  className={`p-0.5 rounded transition-opacity ${
                                    selectedIds.has(item.id)
                                      ? "opacity-100 text-[#0b57d0] dark:text-[#a8c7fa]"
                                      : "opacity-0 group-hover:opacity-100 theme-text-muted"
                                  }`}
                                >
                                  {selectedIds.has(item.id) ? (
                                    <CheckSquare className="w-4 h-4" />
                                  ) : (
                                    <Square className="w-4 h-4" />
                                  )}
                                </button>
                                <div className="shrink-0">{getFileIcon(item)}</div>
                                <span className="text-sm font-medium theme-text-main truncate" title={item.name}>
                                  {item.name}
                                </span>
                              </div>

                              {/* Action Menu with Three Dots */}
                              <div
                                className="flex items-center gap-1 shrink-0 relative"
                                onClick={(e) => e.stopPropagation()}
                              >
                                {currentView !== "trash" ? (
                                  <>
                                    <button
                                      onClick={() => toggleStarItem(item.id).then(loadCurrentViewData)}
                                      className={`p-1 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity ${
                                        item.starred ? "text-amber-500 !opacity-100" : "theme-text-muted hover:theme-text-main"
                                      }`}
                                      title="Star"
                                    >
                                      <Star className="w-3.5 h-3.5 fill-current" />
                                    </button>

                                    {/* Three Dots More Options Menu */}
                                    <div className="relative">
                                      <button
                                        onClick={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                                        className={`p-1 rounded-lg hover:bg-black/10 dark:hover:bg-white/10 transition-colors ${
                                          activeMenuId === item.id
                                            ? "bg-black/10 dark:bg-white/10 text-[#0b57d0] dark:text-[#a8c7fa] opacity-100"
                                            : "theme-text-muted hover:theme-text-main opacity-0 group-hover:opacity-100"
                                        }`}
                                        title="More options"
                                      >
                                        <MoreVertical className="w-4 h-4" />
                                      </button>
                                      {renderThreeDotsMenu(item, true)}
                                    </div>
                                  </>
                                ) : (
                                  <button
                                    onClick={() => {
                                      restoreItemFromTrash(item.id).then(() => {
                                        notifySuccess(`Restored "${item.name}"`);
                                        loadCurrentViewData();
                                        loadQuota();
                                      });
                                    }}
                                    className="p-1.5 rounded-lg hover:bg-emerald-500/15 text-emerald-500"
                                    title="Restore"
                                  >
                                    <RotateCcw className="w-4 h-4" />
                                  </button>
                                )}
                              </div>
                            </div>
                          ))}
                      </div>
                    </div>
                  )}

                  {/* Files Section */}
                  {filteredItems.filter((i) => !i.directory).length > 0 && (
                    <div>
                      {filteredItems.filter((i) => i.directory).length > 0 && (
                        <h3 className="text-sm font-medium theme-text-sub mb-3">Files</h3>
                      )}
                      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                        {filteredItems
                          .filter((i) => !i.directory)
                          .map((item) => (
                            <div
                              key={item.id}
                              className={`group bg-[#f0f4f9]/70 dark:bg-[#282a2c]/70 hover:bg-[#e9eef6] dark:hover:bg-[#333537] border ${
                                selectedIds.has(item.id)
                                  ? "border-[#0b57d0] dark:border-[#a8c7fa] bg-[#c2e7ff]/25"
                                  : "border-[#e0e2ec] dark:border-[#3c4043] hover:border-[#c2e7ff]"
                              } rounded-2xl transition-all flex flex-col justify-between relative cursor-pointer shadow-xs hover:shadow-md`}
                              onClick={() => handleItemPreviewOrOpen(item)}
                            >
                              {/* Top Preview/Icon Area */}
                              <div className="h-32 bg-white/70 dark:bg-black/20 flex items-center justify-center relative p-4 border-b border-[#e0e2ec]/60 dark:border-[#3c4043]/60 rounded-t-2xl overflow-hidden">
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    toggleSelectItem(item.id);
                                  }}
                                  className={`absolute top-2.5 left-2.5 p-1 rounded-md transition-opacity ${
                                    selectedIds.has(item.id)
                                      ? "opacity-100 text-[#0b57d0] dark:text-[#a8c7fa]"
                                      : "opacity-0 group-hover:opacity-100 theme-text-muted hover:theme-text-main"
                                  }`}
                                >
                                  {selectedIds.has(item.id) ? (
                                    <CheckSquare className="w-4 h-4" />
                                  ) : (
                                    <Square className="w-4 h-4" />
                                  )}
                                </button>

                                <div className="scale-125 transform transition-transform group-hover:scale-135">
                                  {getFileIcon(item)}
                                </div>

                                {item.starred && (
                                  <Star className="w-4 h-4 text-amber-500 fill-current absolute top-2.5 right-2.5" />
                                )}
                              </div>

                              {/* Bottom Details Footer */}
                              <div className="p-3.5 flex flex-col justify-between flex-1">
                                <div className="flex items-start justify-between gap-1 mb-1">
                                  <span className="text-xs font-semibold theme-text-main truncate" title={item.name}>
                                    {item.name}
                                  </span>
                                  <div className="relative shrink-0" onClick={(e) => e.stopPropagation()}>
                                    <button
                                      onClick={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                                      className={`p-1 rounded-full hover:bg-black/10 dark:hover:bg-white/10 transition-colors ${
                                        activeMenuId === item.id
                                          ? "bg-black/10 dark:bg-white/10 text-[#0b57d0] dark:text-[#a8c7fa] opacity-100"
                                          : "theme-text-muted hover:theme-text-main opacity-0 group-hover:opacity-100"
                                      }`}
                                      title="More options"
                                    >
                                      <MoreVertical className="w-3.5 h-3.5" />
                                    </button>
                                    {renderThreeDotsMenu(item, true)}
                                  </div>
                                </div>
                                <div className="flex justify-between items-center text-[11px] theme-text-muted mt-2 pt-2 border-t border-[#e0e2ec]/40 dark:border-[#3c4043]/40">
                                  <span>{formatBytes(item.size)}</span>
                                  <span>{new Date(item.updatedAt).toLocaleDateString()}</span>
                                </div>

                                {/* Action Row on Hover */}
                                <div
                                  className="flex items-center justify-end gap-1 mt-2 pt-2 border-t border-[#e0e2ec]/40 dark:border-[#3c4043]/40 opacity-0 group-hover:opacity-100 transition-opacity"
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  {currentView !== "trash" ? (
                                    <>
                                      <button
                                        onClick={() => toggleStarItem(item.id).then(loadCurrentViewData)}
                                        className={`p-1 rounded hover:bg-black/5 dark:hover:bg-white/10 ${
                                          item.starred ? "text-amber-500" : "theme-text-muted hover:theme-text-main"
                                        }`}
                                        title="Star"
                                      >
                                        <Star className="w-3.5 h-3.5 fill-current" />
                                      </button>
                                      <button
                                        onClick={() => handleOpenPublicShare(item)}
                                        className="p-1 rounded hover:bg-[#c2e7ff]/30 text-[#0b57d0] dark:text-[#a8c7fa]"
                                        title="Public link"
                                      >
                                        <Link2 className="w-3.5 h-3.5" />
                                      </button>
                                      <a
                                        href={getFileDownloadUrl(item.id)}
                                        download
                                        className="p-1 rounded hover:bg-black/5 dark:hover:bg-white/10 theme-text-muted hover:theme-text-main"
                                        title="Download"
                                      >
                                        <Download className="w-3.5 h-3.5" />
                                      </a>
                                      <button
                                        onClick={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                                        className="p-1 rounded hover:bg-black/5 dark:hover:bg-white/10 theme-text-muted hover:theme-text-main"
                                        title="More options"
                                      >
                                        <MoreVertical className="w-3.5 h-3.5" />
                                      </button>
                                    </>
                                  ) : (
                                    <button
                                      onClick={() => {
                                        restoreItemFromTrash(item.id).then(() => {
                                          notifySuccess(`Restored "${item.name}"`);
                                          loadCurrentViewData();
                                          loadQuota();
                                        });
                                      }}
                                      className="p-1 rounded hover:bg-emerald-500/15 text-emerald-500"
                                      title="Restore"
                                    >
                                      <RotateCcw className="w-3.5 h-3.5" />
                                    </button>
                                  )}
                                </div>
                              </div>
                            </div>
                          ))}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                /* List View */
                <div className="bg-white dark:bg-[#1e1f20] border border-[#e0e2ec] dark:border-[#3c4043] rounded-2xl overflow-hidden shadow-xs">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-[#f0f4f9] dark:bg-[#282a2c] border-b border-[#e0e2ec] dark:border-[#3c4043] text-xs font-semibold theme-text-sub">
                      <tr>
                        <th className="py-3 px-4 w-10">
                          <button onClick={handleSelectAll}>
                            {selectedIds.size > 0 && selectedIds.size === filteredItems.length ? (
                              <CheckSquare className="w-4 h-4 text-[#0b57d0] dark:text-[#a8c7fa]" />
                            ) : (
                              <Square className="w-4 h-4 theme-text-muted" />
                            )}
                          </button>
                        </th>
                        <th className="py-3 px-4">Name</th>
                        <th className="py-3 px-4">Owner</th>
                        <th className="py-3 px-4">Last modified</th>
                        <th className="py-3 px-4">File size</th>
                        <th className="py-3 px-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#e0e2ec]/60 dark:divide-[#3c4043]/60">
                      {filteredItems.map((item) => (
                        <tr
                          key={item.id}
                          className={`hover:bg-[#f0f4f9] dark:hover:bg-[#282a2c] transition-colors cursor-pointer ${
                            selectedIds.has(item.id) ? "bg-[#c2e7ff]/30 dark:bg-[#004a77]/30" : ""
                          }`}
                          onClick={() => handleItemPreviewOrOpen(item)}
                        >
                          <td className="py-3 px-4" onClick={(e) => e.stopPropagation()}>
                            <button onClick={() => toggleSelectItem(item.id)}>
                              {selectedIds.has(item.id) ? (
                                <CheckSquare className="w-4 h-4 text-[#0b57d0] dark:text-[#a8c7fa]" />
                              ) : (
                                <Square className="w-4 h-4 theme-text-muted" />
                              )}
                            </button>
                          </td>
                          <td className="py-3 px-4 flex items-center gap-3 font-medium theme-text-main">
                            {getFileIcon(item)}
                            <span className="truncate max-w-xs">{item.name}</span>
                            {item.starred && (
                              <Star className="w-3.5 h-3.5 text-amber-500 fill-current shrink-0" />
                            )}
                          </td>
                          <td className="py-3 px-4 theme-text-muted text-xs">
                            me
                          </td>
                          <td className="py-3 px-4 theme-text-muted text-xs">
                            {new Date(item.updatedAt).toLocaleDateString()}
                          </td>
                          <td className="py-3 px-4 theme-text-muted text-xs">
                            {item.directory ? "—" : formatBytes(item.size)}
                          </td>
                          <td className="py-3 px-4 text-right relative" onClick={(e) => e.stopPropagation()}>
                            <div className="flex items-center justify-end gap-1">
                              {currentView !== "trash" ? (
                                <>
                                  <button
                                    onClick={() => toggleStarItem(item.id).then(loadCurrentViewData)}
                                    className={`p-1.5 rounded-full hover:bg-black/5 dark:hover:bg-white/10 ${
                                      item.starred ? "text-amber-500" : "theme-text-muted hover:theme-text-main"
                                    }`}
                                    title="Star"
                                  >
                                    <Star className="w-4 h-4 fill-current" />
                                  </button>
                                  <button
                                    onClick={() => handleOpenPublicShare(item)}
                                    className="p-1.5 rounded-full hover:bg-[#c2e7ff]/30 text-[#0b57d0] dark:text-[#a8c7fa]"
                                    title="Public link"
                                  >
                                    <Link2 className="w-4 h-4" />
                                  </button>
                                  <a
                                    href={item.directory ? getFolderZipDownloadUrl(item.id) : getFileDownloadUrl(item.id)}
                                    download
                                    className="p-1.5 rounded-full hover:bg-black/5 dark:hover:bg-white/10 theme-text-muted hover:theme-text-main inline-block"
                                    title="Download"
                                  >
                                    <Download className="w-4 h-4" />
                                  </a>

                                  {/* Three Dots More Options Menu */}
                                  <div className="relative">
                                    <button
                                      onClick={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                                      className={`p-1.5 rounded-full hover:bg-black/10 dark:hover:bg-white/10 transition-colors ${
                                        activeMenuId === item.id ? "bg-black/10 dark:bg-white/10 text-[#0b57d0] dark:text-[#a8c7fa]" : "theme-text-muted hover:theme-text-main"
                                      }`}
                                      title="More options"
                                    >
                                      <MoreVertical className="w-4 h-4" />
                                    </button>
                                    {renderThreeDotsMenu(item, true)}
                                  </div>
                                </>
                              ) : (
                                <button
                                  onClick={() => {
                                    restoreItemFromTrash(item.id).then(() => {
                                      notifySuccess(`Restored "${item.name}"`);
                                      loadCurrentViewData();
                                      loadQuota();
                                    });
                                  }}
                                  className="p-1.5 rounded-lg hover:bg-emerald-500/15 text-emerald-500"
                                  title="Restore"
                                >
                                  <RotateCcw className="w-4 h-4" />
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </main>
        </div>
      </div>

      {/* MODAL: New Folder */}
      {newFolderOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-lg font-bold theme-text-main mb-4">Create New Folder</h3>
            <form onSubmit={handleCreateFolder}>
              <input
                type="text"
                autoFocus
                placeholder="Folder name"
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                className="w-full theme-bg-input border theme-border rounded-xl px-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] mb-4"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setNewFolderOpen(false)}
                  className="px-4 py-2 theme-bg-subtle hover:theme-bg-hover theme-text-sub text-xs font-semibold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 theme-btn-primary text-xs font-semibold rounded-xl cursor-pointer"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: New Blank Document */}
      {newDocOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-lg font-bold theme-text-main mb-4">New document</h3>
            <form onSubmit={handleCreateDoc}>
              <div className="space-y-3 mb-4">
                <input
                  type="text"
                  autoFocus
                  placeholder="Document name"
                  value={newDocName}
                  onChange={(e) => setNewDocName(e.target.value)}
                  className="w-full theme-bg-input border theme-border rounded-xl px-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)]"
                />
                <select
                  value={newDocExt}
                  onChange={(e) => setNewDocExt(e.target.value)}
                  className="w-full theme-bg-input border theme-border rounded-xl px-3 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                >
                  <option value=".txt">Plain Text (.txt)</option>
                  <option value=".md">Markdown Document (.md)</option>
                  <option value=".js">JavaScript Script (.js)</option>
                  <option value=".html">HTML Webpage (.html)</option>
                  <option value=".json">JSON Data (.json)</option>
                </select>
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setNewDocOpen(false)}
                  className="px-4 py-2 theme-bg-subtle hover:theme-bg-hover theme-text-sub text-xs font-semibold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 theme-btn-primary text-xs font-semibold rounded-xl cursor-pointer"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Rename */}
      {renameItemTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-lg font-bold theme-text-main mb-4">Rename</h3>
            <form onSubmit={handleRename}>
              <input
                type="text"
                autoFocus
                value={renameValue}
                onChange={(e) => setRenameValue(e.target.value)}
                className="w-full theme-bg-input border theme-border rounded-xl px-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] mb-4"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setRenameItemTarget(null)}
                  className="px-4 py-2 theme-bg-subtle hover:theme-bg-hover theme-text-sub text-xs font-semibold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 theme-btn-primary text-xs font-semibold rounded-xl cursor-pointer"
                >
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Move */}
      {moveItemTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-lg font-bold theme-text-main mb-4">Move "{moveItemTarget.name}"</h3>
            <form onSubmit={handleMove}>
              <input
                type="text"
                autoFocus
                placeholder="Destination path"
                value={moveTargetPath}
                onChange={(e) => setMoveTargetPath(e.target.value)}
                className="w-full theme-bg-input border theme-border rounded-xl px-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] mb-4"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setMoveItemTarget(null)}
                  className="px-4 py-2 theme-bg-subtle hover:theme-bg-hover theme-text-sub text-xs font-semibold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 theme-btn-primary text-xs font-semibold rounded-xl cursor-pointer"
                >
                  Move
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Folder Color Picker */}
      {colorPickerTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-lg font-bold theme-text-main mb-4">Folder Color</h3>
            <div className="grid grid-cols-6 gap-3 mb-6">
              {[
                "#3b82f6", // Blue
                "#10b981", // Emerald
                "#f59e0b", // Amber
                "#ef4444", // Rose
                "#8b5cf6", // Purple
                "#06b6d4", // Cyan
              ].map((c) => (
                <button
                  key={c}
                  onClick={() => handleSetColor(c)}
                  className="w-10 h-10 rounded-xl transition-transform hover:scale-110 flex items-center justify-center border border-white/20"
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
            <div className="flex justify-end">
              <button
                onClick={() => setColorPickerTarget(null)}
                className="px-4 py-2 theme-bg-subtle theme-text-sub text-xs font-semibold rounded-xl"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: File Version History */}
      {versionsTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-lg shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold theme-text-main">Version History: {versionsTarget.name}</h3>
              <button onClick={() => setVersionsTarget(null)} className="theme-text-muted hover:theme-text-main">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="max-h-72 overflow-y-auto space-y-2 mb-4">
              <div className="p-3 theme-pill-active rounded-xl flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-[var(--app-primary-light)]">Current Version (v{versionsTarget.versionCount || 1})</span>
                  <p className="text-xs theme-text-muted">{formatBytes(versionsTarget.size)} • Active</p>
                </div>
                <span className="px-2 py-0.5 rounded text-[11px] theme-btn-primary">Live</span>
              </div>
              {versionsList.map((ver) => (
                <div key={ver.id} className="p-3 theme-bg-input border theme-border rounded-xl flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold theme-text-main">Version {ver.versionNumber}</span>
                    <p className="text-xs theme-text-muted">
                      {formatBytes(ver.size)} • {new Date(ver.createdAt).toLocaleString()}
                    </p>
                    {ver.changeSummary && <p className="text-[11px] theme-text-faint mt-0.5">{ver.changeSummary}</p>}
                  </div>
                  <button
                    onClick={() => handleRestoreVersion(ver.id)}
                    className="px-3 py-1 theme-bg-subtle hover:theme-bg-hover text-xs font-semibold theme-text-sub rounded-lg cursor-pointer"
                  >
                    Restore
                  </button>
                </div>
              ))}
              {versionsList.length === 0 && (
                <p className="text-xs theme-text-muted text-center py-4">No previous versions</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Share (Public link & User Share) */}
      {shareItemTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-md shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold theme-text-main">Share "{shareItemTarget.name}"</h3>
              <button onClick={() => setShareItemTarget(null)} className="theme-text-muted hover:theme-text-main">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Share Type Tabs */}
            <div className="flex theme-bg-input p-1 rounded-xl mb-4 border theme-border-subtle">
              <button
                type="button"
                onClick={() => setShareTab("public")}
                className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                  shareTab === "public" ? "theme-btn-primary shadow-sm" : "theme-text-muted hover:theme-text-main"
                }`}
              >
                Public Link
              </button>
              <button
                type="button"
                onClick={() => setShareTab("user")}
                className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                  shareTab === "user" ? "theme-btn-primary shadow-sm" : "theme-text-muted hover:theme-text-main"
                }`}
              >
                Share with User
              </button>
            </div>

            {shareTab === "public" ? (
              !generatedShare ? (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold theme-text-muted mb-1.5">Expiration</label>
                    <select
                      value={shareExpiryHours}
                      onChange={(e) => setShareExpiryHours(Number(e.target.value))}
                      className="w-full theme-bg-input border theme-border rounded-xl px-3 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                    >
                      <option value={24} className="theme-bg-surface">Expires in 24 hours</option>
                      <option value={168} className="theme-bg-surface">Expires in 7 days</option>
                      <option value={720} className="theme-bg-surface">Expires in 30 days</option>
                      <option value={0} className="theme-bg-surface">Never expires</option>
                    </select>
                  </div>
                  <button
                    onClick={handleGeneratePublicShare}
                    className="w-full py-2.5 theme-btn-primary text-sm font-semibold rounded-xl cursor-pointer shadow-md"
                  >
                    Create Link
                  </button>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="p-3 theme-bg-input border theme-border rounded-xl">
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-xs font-semibold theme-text-muted flex items-center gap-1.5">
                        <Link2 className="w-3.5 h-3.5 text-[#0b57d0] dark:text-[#a8c7fa]" />
                        Public Link
                      </span>
                      <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-medium bg-emerald-500/10 px-2 py-0.5 rounded-full">
                        Active
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-2 bg-white dark:bg-[#1e1f20] p-2.5 rounded-lg border theme-border-subtle">
                      <p className="text-xs font-mono text-[#0b57d0] dark:text-[#a8c7fa] truncate select-all">
                        {window.location.origin}/?share={generatedShare.token}
                      </p>
                      <button
                        onClick={() => {
                          const fullUrl = `${window.location.origin}/?share=${generatedShare.token}`;
                          navigator.clipboard.writeText(fullUrl);
                          notifySuccess("Public link copied to clipboard!");
                        }}
                        className="p-1 rounded-md hover:bg-black/5 dark:hover:bg-white/10 text-[#0b57d0] dark:text-[#a8c7fa] shrink-0"
                        title="Copy link"
                      >
                        <Copy className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => {
                        const fullUrl = `${window.location.origin}/?share=${generatedShare.token}`;
                        navigator.clipboard.writeText(fullUrl);
                        notifySuccess("Public link copied to clipboard!");
                      }}
                      className="flex-1 py-2.5 theme-btn-primary text-sm font-semibold rounded-xl flex items-center justify-center gap-2 cursor-pointer shadow-md"
                    >
                      <Copy className="w-4 h-4" />
                      <span>Copy link</span>
                    </button>
                    <a
                      href={`${window.location.origin}/?share=${generatedShare.token}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-2.5 rounded-xl border theme-border hover:theme-bg-hover theme-text-muted hover:theme-text-main transition-colors flex items-center justify-center"
                      title="Open in new tab"
                    >
                      <ExternalLink className="w-4 h-4" />
                    </a>
                  </div>

                  {/* Expiration Settings */}
                  <div className="pt-2 border-t theme-border-subtle">
                    <label className="block text-xs font-semibold theme-text-muted mb-1.5">Change Expiration</label>
                    <div className="flex items-center gap-2">
                      <select
                        value={shareExpiryHours}
                        onChange={(e) => setShareExpiryHours(Number(e.target.value))}
                        className="flex-1 theme-bg-input border theme-border rounded-xl px-3 py-2 text-xs theme-text-main focus:outline-none"
                      >
                        <option value={24} className="theme-bg-surface">Expires in 24 hours</option>
                        <option value={168} className="theme-bg-surface">Expires in 7 days</option>
                        <option value={720} className="theme-bg-surface">Expires in 30 days</option>
                        <option value={0} className="theme-bg-surface">Never expires</option>
                      </select>
                      <button
                        type="button"
                        onClick={handleGeneratePublicShare}
                        className="px-3 py-2 text-xs font-semibold rounded-xl border theme-border hover:theme-bg-hover theme-text-main transition-colors"
                      >
                        Update
                      </button>
                    </div>
                  </div>
                </div>
              )
            ) : (
              /* User-to-User Share Form */
              <form onSubmit={handleShareWithUser} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold theme-text-muted mb-1.5">Recipient</label>
                  <input
                    type="text"
                    required
                    placeholder="Username"
                    value={shareTargetUsername}
                    onChange={(e) => setShareTargetUsername(e.target.value)}
                    className="w-full theme-bg-input border theme-border rounded-xl px-3 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold theme-text-muted mb-1.5">Permission</label>
                  <select
                    value={sharePermission}
                    onChange={(e) => setSharePermission(e.target.value as any)}
                    className="w-full theme-bg-input border theme-border rounded-xl px-3 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                  >
                    <option value="VIEW" className="theme-bg-surface">View & Download</option>
                    <option value="EDIT" className="theme-bg-surface">View, Edit & Save</option>
                  </select>
                </div>
                <button
                  type="submit"
                  className="w-full py-2.5 theme-btn-primary text-sm font-semibold rounded-xl cursor-pointer shadow-md"
                >
                  Share
                </button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* MODAL: Multimedia & PDF Preview Modal */}
      {previewMedia && (
        <div
          className="fixed inset-0 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 z-50"
          onClick={() => setPreviewMedia(null)}
        >
          <div
            className="relative max-w-4xl max-h-[85vh] w-full flex flex-col items-center justify-center theme-bg-surface border theme-border rounded-2xl p-4 overflow-hidden shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-full flex items-center justify-between pb-3 border-b theme-border-subtle mb-3">
              <span className="text-sm font-semibold theme-text-main truncate">{previewMedia.name}</span>
              <button onClick={() => setPreviewMedia(null)} className="theme-text-muted hover:theme-text-main">
                <X className="w-5 h-5" />
              </button>
            </div>

            {previewMedia.type === "image" && (
              <img
                src={previewMedia.url}
                alt={previewMedia.name}
                className="max-w-full max-h-[70vh] rounded-xl object-contain"
              />
            )}

            {previewMedia.type === "video" && (
              <video controls autoPlay className="max-w-full max-h-[70vh] rounded-xl">
                <source src={previewMedia.url} />
                Your browser does not support video playback.
              </video>
            )}

            {previewMedia.type === "audio" && (
              <div className="p-8 w-full flex flex-col items-center">
                <Music className="w-16 h-16 text-purple-400 mb-4 animate-pulse" />
                <audio controls autoPlay className="w-full max-w-md">
                  <source src={previewMedia.url} />
                  Your browser does not support audio playback.
                </audio>
              </div>
            )}

            {previewMedia.type === "pdf" && (
              <iframe src={previewMedia.url} className="w-full h-[70vh] rounded-xl border theme-border" />
            )}
          </div>
        </div>
      )}

      {/* MODAL: Code & Text Editor */}
      {editorItem && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl w-full max-w-4xl h-[80vh] flex flex-col shadow-2xl overflow-hidden">
            <div className="h-14 border-b theme-border-subtle px-6 flex items-center justify-between theme-bg-header">
              <div className="flex items-center gap-2">
                <FileCode className="w-5 h-5 text-[var(--app-primary-light)]" />
                <span className="font-semibold theme-text-main">{editorItem.name}</span>
              </div>
              <div className="flex items-center gap-3">
                <button
                  onClick={handleSaveEditor}
                  disabled={editorSaving}
                  className="px-4 py-1.5 theme-btn-primary text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  {editorSaving ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                  <span>Save Changes (New Version)</span>
                </button>
                <button
                  onClick={() => setEditorItem(null)}
                  className="p-1.5 theme-text-muted hover:theme-text-main rounded-lg theme-bg-hover"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>
            <div className="flex-1 p-4 theme-bg-input font-mono text-sm overflow-hidden flex">
              <textarea
                value={editorItem.content}
                onChange={(e) => setEditorItem({ ...editorItem, content: e.target.value })}
                className="w-full h-full bg-transparent theme-text-sub resize-none outline-none leading-relaxed"
                spellCheck={false}
              />
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Account Settings & Password */}
      {settingsOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="theme-bg-surface border theme-border rounded-2xl p-6 w-full max-w-md shadow-2xl space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold theme-text-main">Account Settings</h3>
              <button onClick={() => setSettingsOpen(false)} className="theme-text-muted hover:theme-text-main">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Appearance Toggle */}
            <div className="flex items-center justify-between pb-4 border-b theme-border-subtle">
              <div>
                <p className="text-sm font-medium theme-text-main">Dark Theme</p>
                <p className="text-xs theme-text-muted">Switch between light and dark mode</p>
              </div>
              <ThemeToggle theme={theme} onToggle={toggleTheme} />
            </div>

            {/* Profile Info Form */}
            <form onSubmit={handleUpdateProfile} className="space-y-3">
              <p className="text-xs font-bold theme-text-muted uppercase tracking-wider">Profile Information</p>
              <div>
                <label className="block text-xs theme-text-muted mb-1">Full Name</label>
                <input
                  type="text"
                  value={profileName}
                  onChange={(e) => setProfileName(e.target.value)}
                  className="w-full theme-bg-input border theme-border rounded-xl px-3 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                />
              </div>
              <div>
                <label className="block text-xs theme-text-muted mb-1">Email</label>
                <input
                  type="email"
                  value={profileEmail}
                  onChange={(e) => setProfileEmail(e.target.value)}
                  className="w-full theme-bg-input border theme-border rounded-xl px-3 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                />
              </div>
              <button
                type="submit"
                className="w-full py-2 theme-bg-subtle hover:theme-bg-hover text-xs font-semibold theme-text-main rounded-xl cursor-pointer"
              >
                Update Profile
              </button>
            </form>

            <div className="border-t theme-border-subtle pt-4">
              <form onSubmit={handleChangePassword} className="space-y-3">
                <p className="text-xs font-bold theme-text-muted uppercase tracking-wider">Security & Password</p>
                <div>
                  <label className="block text-xs theme-text-muted mb-1">Current Password</label>
                  <div className="relative">
                    <input
                      type={showOldPassword ? "text" : "password"}
                      required
                      value={oldPassword}
                      onChange={(e) => setOldPassword(e.target.value)}
                      className="w-full theme-bg-input border theme-border rounded-xl pl-3 pr-10 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                    />
                    <button
                      type="button"
                      onClick={() => setShowOldPassword(!showOldPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 theme-text-muted hover:theme-text-main p-1 rounded-md focus:outline-none cursor-pointer transition-colors"
                      aria-label={showOldPassword ? "Hide current password" : "Show current password"}
                      title={showOldPassword ? "Hide current password" : "Show current password"}
                    >
                      {showOldPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="block text-xs theme-text-muted mb-1">New Password (min 6 chars)</label>
                  <div className="relative">
                    <input
                      type={showNewPassword ? "text" : "password"}
                      required
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="w-full theme-bg-input border theme-border rounded-xl pl-3 pr-10 py-2 text-sm theme-text-main focus:outline-none focus:border-[var(--app-border-focus)]"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 theme-text-muted hover:theme-text-main p-1 rounded-md focus:outline-none cursor-pointer transition-colors"
                      aria-label={showNewPassword ? "Hide new password" : "Show new password"}
                      title={showNewPassword ? "Hide new password" : "Show new password"}
                    >
                      {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full py-2 theme-btn-primary text-xs font-semibold rounded-xl cursor-pointer"
                >
                  Change Password
                </button>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Floating Upload & Progress Popup Widget at Right End */}
      {uploadPopupOpen && uploadQueue.length > 0 && (
        <div className="fixed bottom-5 right-6 z-40 w-80 md:w-96 shadow-2xl rounded-2xl theme-bg-surface border theme-border overflow-hidden animate-in slide-in-from-bottom-5 duration-300">
          {/* Header */}
          <div className="h-12 px-4 theme-bg-subtle border-b theme-border-subtle flex items-center justify-between">
            <div className="flex items-center gap-2">
              {uploadQueue.some((q) => q.status === "uploading") ? (
                <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
              ) : uploadQueue.every((q) => q.status === "completed") ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              ) : (
                <AlertCircle className="w-4 h-4 text-red-500" />
              )}
              <span className="text-sm font-semibold theme-text-main">
                {uploadQueue.some((q) => q.status === "uploading")
                  ? `Uploading ${uploadQueue.length} item${uploadQueue.length > 1 ? "s" : ""}...`
                  : `${uploadQueue.length} upload${uploadQueue.length > 1 ? "s" : ""} complete`}
              </span>
            </div>
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => setUploadPopupMinimized(!uploadPopupMinimized)}
                className="p-1 rounded-md theme-text-muted hover:theme-text-main hover:theme-bg-hover transition-colors cursor-pointer"
                title={uploadPopupMinimized ? "Expand" : "Minimize"}
              >
                {uploadPopupMinimized ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
              </button>
              <button
                type="button"
                onClick={() => setUploadPopupOpen(false)}
                className="p-1 rounded-md theme-text-muted hover:theme-text-main hover:theme-bg-hover transition-colors cursor-pointer"
                title="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Body items (when not minimized) */}
          {!uploadPopupMinimized && (
            <div className="max-h-60 overflow-y-auto divide-y theme-border-subtle p-2">
              {uploadQueue.map((item) => (
                <div key={item.id} className="py-2.5 px-3 flex items-center justify-between gap-3 hover:theme-bg-hover rounded-xl transition-colors">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <FileText className="w-4 h-4 shrink-0 text-blue-500" />
                    <div className="min-w-0">
                      <p className="text-xs font-medium theme-text-main truncate max-w-[180px] md:max-w-[220px]">
                        {item.name}
                      </p>
                      <p className="text-[10px] theme-text-muted">
                        {formatBytes(item.size)}
                      </p>
                    </div>
                  </div>
                  <div className="shrink-0">
                    {item.status === "uploading" ? (
                      <div className="w-5 h-5 border-2 border-[var(--app-primary)] border-t-transparent rounded-full animate-spin" />
                    ) : item.status === "completed" ? (
                      <div className="w-5 h-5 rounded-full bg-emerald-500/15 text-emerald-500 flex items-center justify-center">
                        <Check className="w-3.5 h-3.5 stroke-[2.5]" />
                      </div>
                    ) : (
                      <AlertCircle className="w-4 h-4 text-red-500" />
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
export default App;
