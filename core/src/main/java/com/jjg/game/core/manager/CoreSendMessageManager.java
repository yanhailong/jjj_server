package com.jjg.game.core.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.NoticeMoneyChange;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/11 17:55
 */
@Component
public class CoreSendMessageManager extends BaseSendMessageManager{
    public void packMoneyChangeMessage(PlayerController playerController,long gold, long diamond){
        SendInfo sendInfo = new SendInfo();
        NoticeMoneyChange notice = new NoticeMoneyChange();
        notice.gold = gold;
        notice.diamond = diamond;

        sendInfo.addPlayerMsg(playerController.playerId(), notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(playerController,sendInfo,"推送金钱变化信息",false);
    }
}
