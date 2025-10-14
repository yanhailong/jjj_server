package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.utils.VipUtil;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/8/29 11:32
 */
public class RoomDataHelper {
    private static final Logger log = LoggerFactory.getLogger(RoomDataHelper.class);
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

    public static void checkPlayerVipLevel(Player player, AbstractPhaseGameController<? extends RoomCfg, ? extends GameDataVo<?>> controller, long effectiveWaterFlow) {
        if (player instanceof GameRobotPlayer) {
            return;
        }
        try {
            Map<Integer, ViplevelCfg> viplevelCfgMap = RoomDataHelper.getVipLevelCfgMap();
            ViplevelCfg cfg = viplevelCfgMap.get(player.getVipLevel());
            if (Objects.nonNull(cfg) && cfg.getEffectiveBetting() > 0) {
                if (VipUtil.bettingCheckVipLevel(player, viplevelCfgMap, effectiveWaterFlow)) {
                    NoticeBaseInfoChange noticeBaseInfoChange = new NoticeBaseInfoChange();
                    noticeBaseInfoChange.gold = player.getGold();
                    noticeBaseInfoChange.diamond =  player.getDiamond();
                    noticeBaseInfoChange.level = player.getLevel();
                    noticeBaseInfoChange.levelExp = player.getExp();
                    noticeBaseInfoChange.vipLevel = player.getVipLevel();
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(player.getId(), noticeBaseInfoChange));
                }
            }
        } catch (Exception e) {
            log.error("检查vip等级异常 playerId:{}", player.getId(), e);
        }
    }
}
