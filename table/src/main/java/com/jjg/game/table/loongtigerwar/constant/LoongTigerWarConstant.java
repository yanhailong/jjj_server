package com.jjg.game.table.loongtigerwar.constant;

import com.jjg.game.common.constant.CoreConst;

/**
 * @author 11
 * @date 2025/6/27 18:01
 */
public interface LoongTigerWarConstant {

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "LoongTigerWar";
        //最大历史记录
        int MAX_HISTORY = 50;
        //龙
        int LOONG_AREA = 1;
        //虎
        int TIGER_TIGER = 2;
        //幸运区域
        int LUCK_AREA = 3;
    }
}
