package com.jjg.game.season.data;

/**
 * 宝石合成结果。
 */
public class SeasonCraftResult {
    private boolean success;
    private int resultItemId;
    private int resultCount;
    private int keptItemId;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getResultItemId() { return resultItemId; }
    public void setResultItemId(int resultItemId) { this.resultItemId = resultItemId; }
    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }
    public int getKeptItemId() { return keptItemId; }
    public void setKeptItemId(int keptItemId) { this.keptItemId = keptItemId; }
}
