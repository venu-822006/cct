package com.clouddrive.dto;

public class StorageBreakdownDTO {
    private long documentsBytes = 0L;
    private long imagesBytes = 0L;
    private long videosBytes = 0L;
    private long audioBytes = 0L;
    private long codeBytes = 0L;
    private long archivesBytes = 0L;
    private long otherBytes = 0L;
    private long trashBytes = 0L;
    private long totalUsedBytes = 0L;
    private long totalQuotaBytes = 0L;

    public StorageBreakdownDTO() {}

    public long getDocumentsBytes() { return documentsBytes; }
    public void setDocumentsBytes(long documentsBytes) { this.documentsBytes = documentsBytes; }

    public long getImagesBytes() { return imagesBytes; }
    public void setImagesBytes(long imagesBytes) { this.imagesBytes = imagesBytes; }

    public long getVideosBytes() { return videosBytes; }
    public void setVideosBytes(long videosBytes) { this.videosBytes = videosBytes; }

    public long getAudioBytes() { return audioBytes; }
    public void setAudioBytes(long audioBytes) { this.audioBytes = audioBytes; }

    public long getCodeBytes() { return codeBytes; }
    public void setCodeBytes(long codeBytes) { this.codeBytes = codeBytes; }

    public long getArchivesBytes() { return archivesBytes; }
    public void setArchivesBytes(long archivesBytes) { this.archivesBytes = archivesBytes; }

    public long getOtherBytes() { return otherBytes; }
    public void setOtherBytes(long otherBytes) { this.otherBytes = otherBytes; }

    public long getTrashBytes() { return trashBytes; }
    public void setTrashBytes(long trashBytes) { this.trashBytes = trashBytes; }

    public long getTotalUsedBytes() { return totalUsedBytes; }
    public void setTotalUsedBytes(long totalUsedBytes) { this.totalUsedBytes = totalUsedBytes; }

    public long getTotalQuotaBytes() { return totalQuotaBytes; }
    public void setTotalQuotaBytes(long totalQuotaBytes) { this.totalQuotaBytes = totalQuotaBytes; }
}
