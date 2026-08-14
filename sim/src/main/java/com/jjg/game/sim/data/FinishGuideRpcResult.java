package com.jjg.game.sim.data;

import java.util.Collections;
import java.util.List;

/** 跨节点完成单个新手引导步骤的处理结果。 */
public class FinishGuideRpcResult {
    /** 处理状态码。 */
    public int code;
    /** 完成的引导步骤ID。 */
    public int guideId;
    /** 本次完成后由条件8首次触发的后续引导组。 */
    public List<Integer> triggeredGuideGroupIds = Collections.emptyList();

    public FinishGuideRpcResult() {
    }

    public FinishGuideRpcResult(int code, int guideId, List<Integer> triggeredGuideGroupIds) {
        this.code = code;
        this.guideId = guideId;
        this.triggeredGuideGroupIds = triggeredGuideGroupIds == null
                ? Collections.emptyList() : triggeredGuideGroupIds;
    }
}
