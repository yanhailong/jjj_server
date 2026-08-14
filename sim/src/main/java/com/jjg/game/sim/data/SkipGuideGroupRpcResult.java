package com.jjg.game.sim.data;

import java.util.Collections;
import java.util.List;

/** 跨节点跳过整个新手引导组的处理结果。 */
public class SkipGuideGroupRpcResult {
    /** 处理状态码。 */
    public int code;
    /** 跳过的引导组ID。 */
    public int guideGroupId;
    /** 被置为完成的组内引导步骤ID。 */
    public List<Integer> completedGuideIds = Collections.emptyList();
    /** 本次跳过后由条件8首次触发的后续引导组。 */
    public List<Integer> triggeredGuideGroupIds = Collections.emptyList();

    public SkipGuideGroupRpcResult() {
    }

    public SkipGuideGroupRpcResult(int code, int guideGroupId, List<Integer> completedGuideIds,
                                   List<Integer> triggeredGuideGroupIds) {
        this.code = code;
        this.guideGroupId = guideGroupId;
        this.completedGuideIds = completedGuideIds == null
                ? Collections.emptyList() : completedGuideIds;
        this.triggeredGuideGroupIds = triggeredGuideGroupIds == null
                ? Collections.emptyList() : triggeredGuideGroupIds;
    }
}
