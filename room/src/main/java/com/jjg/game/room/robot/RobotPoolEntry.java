package com.jjg.game.room.robot;

import com.jjg.game.sampledata.bean.RobotCfg;

import java.util.Map;

/**
 * 机器人池中的单个机器人快照。
 *
 * @param robotId     当前节点内生成后的真实机器人ID
 * @param robotCfg    机器人配置
 * @param itemAmounts itemId -> 本次入池随机到的携带数量
 */
public record RobotPoolEntry(long robotId, RobotCfg robotCfg, Map<Integer, Long> itemAmounts) {

    /**
     * 获取机器人指定货币的携带数量。
     */
    public long getAmount(int itemId) {
        return itemAmounts.getOrDefault(itemId, 0L);
    }
}
