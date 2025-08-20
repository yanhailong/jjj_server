package com.jjg.game.core.data;

/**
 * 押注类好友房间
 *
 * @author 2CL
 */
public class BetFriendRoom extends FriendRoom {

    // 房间的庄家ID
    private long bankerId;

    public long getBankerId() {
        return bankerId;
    }

    public void setBankerId(long bankerId) {
        this.bankerId = bankerId;
    }
}
