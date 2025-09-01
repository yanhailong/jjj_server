package com.jjg.game.core.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.data.Player;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ViplevelCfg;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * vip工具类
 *
 * @author lm
 * @date 2025/8/29 10:52
 */
public class VipUtil {

    public static boolean rechargeCheckVipLevel(Player player, Map<Integer, ViplevelCfg> viplevelCfgMap, long addValue) {
        return checkVipLevel(player, viplevelCfgMap, addValue, true);
    }

    public static boolean bettingCheckVipLevel(Player player, Map<Integer, ViplevelCfg> viplevelCfgMap, long addValue) {
        return checkVipLevel(player, viplevelCfgMap, addValue, false);
    }

    public static boolean checkVipLevel(Player player, Map<Integer, ViplevelCfg> viplevelCfgMap, long addValue, boolean recharge) {
        if (CollectionUtil.isEmpty(viplevelCfgMap)) {
            return false;
        }
        //进行经验升级
        int newLv = player.getVipLevel();
        long newExp = player.getVipExp();
        boolean chenge = false;
        for (int i = 0; i < viplevelCfgMap.size(); i++) {
            ViplevelCfg viplevelCfg = viplevelCfgMap.get(newLv);
            if (Objects.isNull(viplevelCfg)) {
                break;
            }
            int coefficient = recharge ? viplevelCfg.getRecharge() : viplevelCfg.getEffectiveBetting();
            if (coefficient == 0) {
                break;
            }
            long needEffectiveWaterFlow = (viplevelCfg.getViplevelUpExp() - newExp) * 10000 / coefficient;
            if (addValue >= needEffectiveWaterFlow) {
                addValue -= needEffectiveWaterFlow;
                newExp = 0;
                newLv++;
                chenge = true;
            } else {
                newExp += addValue * coefficient / 10000;
                addValue = 0;
            }
            if (addValue == 0) {
                break;
            }
        }
        if (chenge || newExp != player.getVipExp()) {
            player.setVipExp(newExp);
            player.setVipLevel(newLv);
        }
        return chenge;
    }

    /**
     * 检查vip等级
     *
     * @param player 玩家信息
     * @param num    数量
     * @return ture 等级变化 false 等级没变化
     */
    public static boolean checkVipLevel(Player player, long num) {
        Map<Integer, ViplevelCfg> viplevelCfgMap = GameDataManager.getViplevelCfgList()
                .stream()
                .collect(Collectors.toMap(ViplevelCfg::getViplevel, cfg -> cfg));
        return bettingCheckVipLevel(player, viplevelCfgMap, num);
    }
}
