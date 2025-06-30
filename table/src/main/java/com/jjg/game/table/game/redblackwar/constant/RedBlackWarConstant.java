package com.jjg.game.table.game.redblackwar.constant;

import com.jjg.game.common.constant.CoreConst;

/**
 * @author 11
 * @date 2025/6/27 17:55
 */
public interface RedBlackWarConstant {
    interface GameType{
        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {CoreConst.GameType.RED_BLACK_WAR};
    }

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "redblackwar";
    }
}
