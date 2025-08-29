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
    public static void checkVipLevel(Player player, Map<Integer, ViplevelCfg> viplevelCfgMap) {
        if (CollectionUtil.isEmpty(viplevelCfgMap)) {
            return;
        }
        //进行经验升级
        int newLv = player.getVipLevel();
        long newExp = player.getVipExp();
        for (int i = 0; i < viplevelCfgMap.size(); i++) {
            ViplevelCfg viplevelCfg = viplevelCfgMap.get(newLv);
            if (Objects.isNull(viplevelCfg)) {
                return;
            }
            if (player.getVipExp() > viplevelCfg.getViplevelUpExp()) {
                newExp -= viplevelCfg.getViplevelUpExp();
                newLv++;
            }
        }
        if (newLv != player.getVipLevel() && newExp != player.getVipExp()) {
            player.setVipExp(newExp);
            player.setVipLevel(newLv);
        }
    }
    public static void checkVipLevel(Player player) {
        Map<Integer, ViplevelCfg> viplevelCfgMap = GameDataManager.getViplevelCfgList()
                .stream()
                .collect(Collectors.toMap(ViplevelCfg::getViplevel, cfg -> cfg));
        checkVipLevel(player,viplevelCfgMap);
    }
}
