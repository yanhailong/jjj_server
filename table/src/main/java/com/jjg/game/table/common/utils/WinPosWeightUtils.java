package com.jjg.game.table.common.utils;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 2CL
 */
public class WinPosWeightUtils {

    /**
     * 通过权重随机一个winPosWeightCfg
     */
    public static WinPosWeightCfg randomCfgByWeight(EGameType gameType) {
        Map<WinPosWeightCfg, Integer> cfgMap = GameDataManager.getWinPosWeightCfgList()
            .stream()
            .filter(cfg -> gameType.getGameTypeId() == cfg.getGameID())
            .collect(Collectors.toMap(cfg -> cfg, WinPosWeightCfg::getPosWeight));
        Set<WinPosWeightCfg> randomWinPosWeightId = RandomUtils.getRandomByWeight(cfgMap, 1);
        if (randomWinPosWeightId.isEmpty()) {
            return null;
        }
        return (WinPosWeightCfg) randomWinPosWeightId.toArray()[0];
    }
}
