package com.jjg.game.core.utils;

import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * 数据处理工具
 *
 * @author 2CL
 */
public class SampleDataUtils {

    /**
     * 通过房间配置获取最大限制
     */
    public static Tuple2<Integer, Integer> getRoomMaxLimit(WarehouseCfg warehouseCfg) {
        String participantsMax = warehouseCfg.getParticipants_max();
        String[] participantsMaxStrArr = participantsMax.split(":");
        return Tuples.of(Integer.parseInt(participantsMaxStrArr[0]), Integer.parseInt(participantsMaxStrArr[1]));
    }

    /**
     * 通过全局表ID获取表中对应的int字段值
     */
    public static int getIntGlobalData(int globalId) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(globalId);
        if (globalConfigCfg == null) {
            throw new GameSampleException("全局表找不到配置ID：" + globalId);
        }
        return globalConfigCfg.getIntValue();
    }
}
