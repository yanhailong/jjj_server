package com.jjg.game.season.data;

/** 宝石合成结果。 */
public class SeasonCraftResult {
    private int materialQuality;
    private boolean success;
    private int resultItemId;
    private int resultCount;
    private int failKeepItemId;

    public int getMaterialQuality() { return materialQuality; }
    public void setMaterialQuality(int materialQuality) { this.materialQuality = materialQuality; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getResultItemId() { return resultItemId; }
    public void setResultItemId(int resultItemId) { this.resultItemId = resultItemId; }
    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }
    public int getFailKeepItemId() { return failKeepItemId; }
    public void setFailKeepItemId(int failKeepItemId) { this.failKeepItemId = failKeepItemId; }
}
