package com.jjg.game.poker.game.blackjack.data;

import com.jjg.game.poker.game.blackjack.message.bean.BlackJackCardInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSendCardInfo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;

/**
 * @author lm
 * @date 2025/8/6 14:55
 */
public class BlackJackBuilder {
    private BlackJackBuilder(){}
    public static NotifyBlackJackSendCardInfo getNotifySendCardInfo(PlayerSeatInfo info, long operationId, long overTime) {

        return notifyBlackJackSendCardInfo;
    }
}
