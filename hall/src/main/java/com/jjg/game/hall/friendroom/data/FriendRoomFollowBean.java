package com.jjg.game.hall.friendroom.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 房间好友数据bean
 *
 * @author 2CL
 */
@Document
@CompoundIndex(
    name = "playerId_followedPlayerId_invitationCode",
    def = "{'playerId':1,'followedPlayerId':1,'invitationCode':1}"
)
public class FriendRoomFollowBean {
    @Id
    private long id;
    // 玩家ID
    private long playerId;
    // 邀请码
    private int invitationCode;
    // 关注的好友ID
    private long followedPlayerId;
    // 关注时间
    private long followedTimeStamp;
    // 置顶时间
    private long topUpTimeStamp;
    // 删除时间
    private long removeTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(int invitationCode) {
        this.invitationCode = invitationCode;
    }

    public long getFollowedPlayerId() {
        return followedPlayerId;
    }

    public void setFollowedPlayerId(long followedPlayerId) {
        this.followedPlayerId = followedPlayerId;
    }

    public long getFollowedTimeStamp() {
        return followedTimeStamp;
    }

    public void setFollowedTimeStamp(long followedTimeStamp) {
        this.followedTimeStamp = followedTimeStamp;
    }

    public long getTopUpTimeStamp() {
        return topUpTimeStamp;
    }

    public void setTopUpTimeStamp(long topUpTimeStamp) {
        this.topUpTimeStamp = topUpTimeStamp;
    }

    public long getRemoveTime() {
        return removeTime;
    }

    public void setRemoveTime(long removeTime) {
        this.removeTime = removeTime;
    }
}
