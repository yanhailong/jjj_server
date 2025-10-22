package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.ActivityItemDropInfo;

import java.util.List;

/**
 * 物品掉落接口
 *
 * @author lm
 * @date 2025/10/21 15:41
 */
public interface DropItemListener extends IGameSysFuncInterface {
    /**
     * 道具掉落
     *
     * @param player 玩家
     * @param param  掉落参数
     * @return 掉落的道具
     */
    List<ActivityItemDropInfo> dropItem(Player player, Object param);
}
