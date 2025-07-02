package com.jjg.game.table.loongtigerwar.constant;

import com.jjg.game.common.constant.CoreConst;

/**
 * @author 11
 * @date 2025/6/27 18:01
 */
public interface LoongTigerWarConstant {
    interface GameType{
        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {CoreConst.GameType.LOONG_TIGER_WAR};
    }

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "LoongTigerWar";
    }
}
