package com.clouddrive.dto;

public class QuotaDTO {
    private long usedBytes;
    private long quotaBytes;
    private double usedPercentage;

    public QuotaDTO() {}

    public QuotaDTO(long usedBytes, long quotaBytes) {
        this.usedBytes = usedBytes;
        this.quotaBytes = quotaBytes;
        this.usedPercentage = quotaBytes > 0 ? ((double) usedBytes / quotaBytes) * 100.0 : 0.0;
    }

    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

    public long getQuotaBytes() { return quotaBytes; }
    public void setQuotaBytes(long quotaBytes) { this.quotaBytes = quotaBytes; }

    public double getUsedPercentage() { return usedPercentage; }
    public void setUsedPercentage(double usedPercentage) { this.usedPercentage = usedPercentage; }
}
