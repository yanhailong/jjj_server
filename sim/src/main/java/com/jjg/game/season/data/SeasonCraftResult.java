package com.jjg.game.season.data;

/**
 * 宝石合成第一步(发起合成)的结果。合成失败时仅 success=false，
 * 保留哪件宝石由第二步 {@link com.jjg.game.season.service.SeasonGemService#craftKeep} 决定。
 */
public class SeasonCraftResult {
    private boolean success;
    private int resultItemId;
    private int resultCount;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getResultItemId() { return resultItemId; }
    public void setResultItemId(int resultItemId) { this.resultItemId = resultItemId; }
    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }
}
