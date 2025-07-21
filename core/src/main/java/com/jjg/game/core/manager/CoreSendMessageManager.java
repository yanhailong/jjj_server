package com.jjg.game.core.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/11 17:55
 */
@Component
public class CoreSendMessageManager extends BaseSendMessageManager{
    public void packMoneyChangeMessage(PlayerController playerController,long gold, long diamond,int vipLevel){
        SendInfo sendInfo = new SendInfo();
        NoticeBaseInfoChange notice = new NoticeBaseInfoChange();
        notice.gold = gold;
        notice.diamond = diamond;
        notice.vipLevel = vipLevel;

        sendInfo.addPlayerMsg(playerController.playerId(), notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(playerController,sendInfo,"推送金钱变化信息",false);
    }
}
