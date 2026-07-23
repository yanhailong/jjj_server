package com.jjg.game.season.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 合成失败后的待结算态(落库)。
 *
 * <p>第一步发起合成掷点失败时，服务端已扣除赛季币、把全部材料从背包扣除(托管)、并卸下已不在背包的镶嵌，
 * 仅把结算所需信息保存在这里。之后：</p>
 * <ul>
 *   <li>玩家第二步选择保留的宝石 → 按 {@link #keepAmount} 返还该宝石；</li>
 *   <li>玩家掉线/登出、或进程异常后重登 → 默认保留材料中的第一件。</li>
 * </ul>
 *
 * <p>只有返还成功才清除本记录；返还失败则保留、待后续(重登)重试，避免玩家永久损失应保留的宝石。
 * 因材料已在第一步扣除，待结算期间玩家无法再消耗/转移这些材料。</p>
 */
public class SeasonPendingCraft {
    /** 已托管(第一步从背包扣除)的材料道具ID列表，按提交顺序，自动结算时默认保留其中第一件。 */
    private List<Integer> itemIds = new ArrayList<>();
    /** 保留所选宝石的数量(取自合成配置 failKeepAmount)，实际返还量再按材料中该宝石的持有量截断。 */
    private int keepAmount;

    public SeasonPendingCraft() {
    }

    public SeasonPendingCraft(List<Integer> itemIds, int keepAmount) {
        this.itemIds = itemIds;
        this.keepAmount = keepAmount;
    }

    public List<Integer> getItemIds() {
        if (itemIds == null) itemIds = new ArrayList<>();
        return itemIds;
    }
    public void setItemIds(List<Integer> itemIds) { this.itemIds = itemIds == null ? new ArrayList<>() : itemIds; }
    public int getKeepAmount() { return keepAmount; }
    public void setKeepAmount(int keepAmount) { this.keepAmount = keepAmount; }
}
