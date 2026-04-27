package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * poker类好友房
 *
 * @author 2CL
 */
@Document(collection = "PokerFriendRoom")
public class PokerFriendRoom extends FriendRoom {
    //是否发送暂停续费的邮件
    private boolean sendPauseRenewalMail;

    public boolean isSendPauseRenewalMail() {
        return sendPauseRenewalMail;
    }

    public void setSendPauseRenewalMail(boolean sendPauseRenewalMail) {
        this.sendPauseRenewalMail = sendPauseRenewalMail;
    }
}
