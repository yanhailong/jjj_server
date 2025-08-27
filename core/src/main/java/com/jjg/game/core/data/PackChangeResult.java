package com.jjg.game.core.data;


/**
 * @param player        更新后的玩家数据 更新钻石 金币会有值
 * @param changGold     变化的金币
 * @param changeDiamond 变化的钻石
 * @author lm
 * @date 2025/8/27 15:48
 */
public record PackChangeResult(Player player
        , long changGold
        , long changeDiamond) {
}
