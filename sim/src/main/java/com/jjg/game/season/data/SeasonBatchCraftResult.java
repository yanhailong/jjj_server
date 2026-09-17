package com.jjg.game.season.data;

import java.util.Map;

/** 批量宝石合成汇总结果。 */
public class SeasonBatchCraftResult {
    private int craftCount;
    private int successCount;
    private Map<Integer, Long> craftCountsByQuality;
    private Map<Integer, Long> consumedItems;
    private Map<Integer, Long> resultItems;
    private Map<Integer, Long> failKeepItems;

    public int getCraftCount() { return craftCount; }
    public void setCraftCount(int craftCount) { this.craftCount = craftCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public Map<Integer, Long> getCraftCountsByQuality() { return craftCountsByQuality; }
    public void setCraftCountsByQuality(Map<Integer, Long> craftCountsByQuality) { this.craftCountsByQuality = craftCountsByQuality; }
    public Map<Integer, Long> getConsumedItems() { return consumedItems; }
    public void setConsumedItems(Map<Integer, Long> consumedItems) { this.consumedItems = consumedItems; }
    public Map<Integer, Long> getResultItems() { return resultItems; }
    public void setResultItems(Map<Integer, Long> resultItems) { this.resultItems = resultItems; }
    public Map<Integer, Long> getFailKeepItems() { return failKeepItems; }
    public void setFailKeepItems(Map<Integer, Long> failKeepItems) { this.failKeepItems = failKeepItems; }
}
