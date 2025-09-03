package com.jjg.game.core.manager;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
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

    public void packMoneyChangeMessage(PlayerController playerController, long gold, long diamond, int vipLevel) {
        SendInfo sendInfo = new SendInfo();
        NoticeBaseInfoChange notice = new NoticeBaseInfoChange();
        notice.gold = gold;
        notice.diamond = diamond;
        notice.vipLevel = vipLevel;

        sendInfo.addPlayerMsg(playerController.playerId(), notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(playerController, sendInfo, "推送货币变化信息", false);
    }

    /**
     * 构建玩家货币信息
     */
    public void buildPlayerMoneyInfo(Player player) {
        PFSession session = playerSessionService.getSession(player.getId());
        if(session == null) {
            return;
        }

        NoticeBaseInfoChange notice = new NoticeBaseInfoChange();
        notice.gold = player.getGold();
        notice.diamond = player.getDiamond();
        notice.vipLevel = player.getVipLevel();
        session.send(notice);
    }
}
