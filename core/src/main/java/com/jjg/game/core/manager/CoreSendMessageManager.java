package com.jjg.game.core.manager;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.service.AbstractPlayerService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/11 17:55
 */
@Component
public class CoreSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private PlayerSessionService playerSessionService;

    /**
     * 推送金币等信息变化
     * @param playerController
     */
    public void buildMoneyChangeMessage(PlayerController playerController) {
        buildMoneyChangeMessage(playerController,playerController.getPlayer().getGold(),playerController.getPlayer().getDiamond(),
                playerController.getPlayer().getVipLevel());
    }

    /**
     * 推送金币等信息变化
     * @param playerController
     */
    public void buildMoneyChangeMessage(PlayerController playerController, long gold, long diamond, int vipLevel) {
        buildMoneyChangeMessage(playerController.getSession(),gold,diamond,vipLevel);
    }

    /**
     * 构建玩家货币信息
     */
    public void buildMoneyChangeMessage(Player player) {
        PFSession session = playerSessionService.getSession(player.getId());
        if(session == null) {
            return;
        }
        buildMoneyChangeMessage(session,player.getGold(),player.getDiamond(),player.getVipLevel());
    }

    /**
     * 构建玩家货币信息
     */
    public void buildMoneyChangeMessage(long playerId, AbstractPlayerService playerService) {
        PFSession session = playerSessionService.getSession(playerId);
        if(session == null) {
            return;
        }
        Player player = playerService.get(playerId);
        buildMoneyChangeMessage(session,player.getGold(),player.getDiamond(),player.getVipLevel());
    }

    /**
     * 推送金币等信息变化
     * @param session
     * @param gold
     * @param diamond
     * @param vipLevel
     */
    public void buildMoneyChangeMessage(PFSession session, long gold, long diamond, int vipLevel) {
        SendInfo sendInfo = new SendInfo();

        NoticeBaseInfoChange notice = new NoticeBaseInfoChange();
        notice.gold = gold;
        notice.diamond = diamond;
        notice.vipLevel = vipLevel;
        session.send(notice);

        sendInfo.addPlayerMsg(session.playerId, notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(session, sendInfo, "推送货币变化信息", false);
    }
}
