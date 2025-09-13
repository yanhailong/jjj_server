package com.jjg.game.room.message;

import com.jjg.game.core.data.Player;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.struct.BasePlayerInfo;

/**
 * 好有房消息builder
 *
 * @author 2CL
 */
public class FriendRoomMessageBuilder {

    /**
     * 构建好友房数据
     */
    public static BasePlayerInfo buildFriendRoomPlayerInfo(GamePlayer player) {
        BasePlayerInfo basePlayerInfo = new BasePlayerInfo();
        basePlayerInfo.playerId = player.getId();
        basePlayerInfo.playerName = player.getNickName();
        basePlayerInfo.titleId = player.getTitleId();
        basePlayerInfo.headImgId = player.getHeadImgId();
        basePlayerInfo.nationalId = player.getNationalId();
        basePlayerInfo.vipLevel = player.getVipLevel();
        basePlayerInfo.gender = player.getGender();
        basePlayerInfo.headFrameId = player.getHeadFrameId();
        basePlayerInfo.goldNum = player.getDiamond();
        return basePlayerInfo;
    }
}
