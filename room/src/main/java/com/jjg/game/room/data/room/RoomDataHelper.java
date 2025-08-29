package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.utils.VipUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ViplevelCfg;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/8/29 11:32
 */
public class RoomDataHelper {
    // vip配置缓存
    private static Map<Integer, ViplevelCfg> VIP_LEVEL_CFG_MAP = new HashMap<>();

    public static void initVipLevelCfg() {
        VIP_LEVEL_CFG_MAP = GameDataManager.getViplevelCfgList()
                .stream()
                .collect(Collectors.toMap(ViplevelCfg::getViplevel, cfg -> cfg));
    }

    public static Map<Integer, ViplevelCfg> getVipLevelCfgMap() {
        return VIP_LEVEL_CFG_MAP;
    }

    public static void checkPlayerVipLevel(Player player, long effectiveWaterFlow) {
        Map<Integer, ViplevelCfg> viplevelCfgMap = RoomDataHelper.getVipLevelCfgMap();
        ViplevelCfg cfg = viplevelCfgMap.get(player.getVipLevel());
        if (Objects.nonNull(cfg) && cfg.getEffectiveBetting() > 0) {
            long addExp = effectiveWaterFlow * cfg.getEffectiveBetting() / 10000;
            player.setVipExp(player.getVipExp() + addExp);
            VipUtil.checkVipLevel(player, viplevelCfgMap);
        }
    }
}
