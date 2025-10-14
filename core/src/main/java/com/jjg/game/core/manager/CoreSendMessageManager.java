package com.jjg.game.core.manager;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.service.AbstractPlayerService;
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
     * @param playerController 玩家控制器
     */
    public void buildMoneyChangeMessage(PlayerController playerController) {
        buildMoneyChangeMessage(playerController,playerController.getPlayer());
    }

    /**
     * 推送金币等信息变化
     * @param playerController 玩家控制器
     */
    public void buildMoneyChangeMessage(PlayerController playerController,Player player) {
        buildMoneyChangeMessage(playerController.getSession(),player);
    }

    /**
     * 构建玩家货币信息
     */
    public void buildMoneyChangeMessage(Player player) {
        PFSession session = playerSessionService.getSession(player.getId());
        if(session == null) {
            return;
        }
        buildMoneyChangeMessage(session,player);
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
        buildMoneyChangeMessage(session,player);
    }

    /**
     * 推送金币等信息变化
     * @param session 玩家连接session
     * @param player 玩家信息
     */
    public void buildMoneyChangeMessage(PFSession session, Player player) {
        SendInfo sendInfo = new SendInfo();

        NoticeBaseInfoChange notice = new NoticeBaseInfoChange();
        notice.gold = player.getGold();
        notice.diamond = player.getDiamond();
        notice.vipLevel = player.getVipLevel();
        notice.level = player.getLevel();
        notice.levelExp = player.getExp();

        sendInfo.addPlayerMsg(session.playerId, notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(session, sendInfo, "推送玩家基础信息", false);
    }
}
