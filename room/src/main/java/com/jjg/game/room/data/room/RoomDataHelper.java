package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.utils.MessageBuildUtil;
import com.jjg.game.core.utils.VipManager;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * @author lm
 * @date 2025/8/29 11:32
 */
public class RoomDataHelper {
    private static final Logger log = LoggerFactory.getLogger(RoomDataHelper.class);

    public static void checkPlayerVipLevel(Player player, AbstractPhaseGameController<? extends RoomCfg, ? extends GameDataVo<?>> controller, long effectiveWaterFlow) {
        if (player instanceof GameRobotPlayer) {
            return;
        }
        try {
            Map<Integer, ViplevelCfg> viplevelCfgMap = VipManager.getVipLevelCfgMap();
            ViplevelCfg cfg = viplevelCfgMap.get(player.getVipLevel());
            if (Objects.nonNull(cfg) && cfg.getEffectiveBetting() > 0) {
                if (VipManager.bettingCheckVipLevel(player, effectiveWaterFlow)) {
                    NoticeBaseInfoChange notice = MessageBuildUtil.buildNoticeBaseInfoChange(player);
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(player.getId(), notice));
                }
            }
        } catch (Exception e) {
            log.error("检查vip等级异常 playerId:{}", player.getId(), e);
        }
    }
}
