package com.jjg.game.hall.utils;

import com.jjg.game.sampledata.bean.WarehouseCfg;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * 数据处理工具
 *
 * @author 2CL
 */
public class HallDataUtils {

    /**
     * 通过房间配置获取最大限制
     */
    public static Tuple2<Integer, Integer> getRoomMaxLimit(WarehouseCfg warehouseCfg) {
        String participantsMax = warehouseCfg.getParticipants_max();
        String[] participantsMaxStrArr = participantsMax.split(":");
        return Tuples.of(Integer.parseInt(participantsMaxStrArr[0]), Integer.parseInt(participantsMaxStrArr[1]));
    }
}
