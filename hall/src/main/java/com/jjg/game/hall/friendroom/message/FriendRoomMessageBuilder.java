package com.jjg.game.hall.friendroom.message;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.Player;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import reactor.util.function.Tuple2;

/**
 * 好友房消息builder
 *
 * @author 2CL
 */
public class FriendRoomMessageBuilder {

    /**
     * 构建好友房数据
     */
    public static BaseFriendRoomPlayerInfo buildFriendRoomPlayerInfo(Player player) {
        BaseFriendRoomPlayerInfo baseFriendRoomPlayerInfo = new BaseFriendRoomPlayerInfo();
        baseFriendRoomPlayerInfo.playerId = player.getId();
        baseFriendRoomPlayerInfo.playerName = player.getNickName();
        baseFriendRoomPlayerInfo.playerTitleId = player.getTitleId();
        baseFriendRoomPlayerInfo.playerHeadIcon = player.getHeadImgId();
        baseFriendRoomPlayerInfo.level = player.getLevel();
        baseFriendRoomPlayerInfo.nationalId = player.getNationalId();
        baseFriendRoomPlayerInfo.playerVipLevel = player.getVipLevel();
        baseFriendRoomPlayerInfo.gender = player.getGender();
        baseFriendRoomPlayerInfo.invitationCode = player.getFriendRoomInvitationCode();
        return baseFriendRoomPlayerInfo;
    }

    /**
     * 构建好友房基础数据
     */
    public static FriendRoomBaseData buildFriendRoomBaseData(FriendRoom friendRoom) {
        FriendRoomBaseData friendRoomBaseData = new FriendRoomBaseData();
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(friendRoom.getRoomCfgId());
        friendRoomBaseData.roomId = friendRoom.getId();
        friendRoomBaseData.roomAliasName = friendRoom.getAliasName();
        // 默认是暂停状态
        friendRoomBaseData.roomStatus = friendRoom.getStatus() == 0 ? 1 : friendRoom.getStatus();
        friendRoomBaseData.onlinePlayerNum = friendRoom.getRoomPlayers().size();
        friendRoomBaseData.gameId = friendRoom.getRoomCfgId();
        friendRoomBaseData.overdueTime = getRoomResetTime(friendRoom);
        friendRoomBaseData.predictCostGoldNum = friendRoom.getPredictCostGoldNum();
        friendRoomBaseData.autoRenewal = friendRoom.isAutoRenewal();
        GlobalConfigCfg globalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
        int intervalTime = globalConfigCfg.getIntValue() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        long curTime = System.currentTimeMillis();
        friendRoomBaseData.nextPauseBtnOverdueTime =
            friendRoom.getPauseTime() + intervalTime > curTime ? friendRoom.getPauseTime() + intervalTime : 0;
        Tuple2<Integer, Integer> roomMaxLimitCfg = SampleDataUtils.getRoomMaxLimit(warehouseCfg);
        friendRoomBaseData.maxPlayerNum = roomMaxLimitCfg.getT2();
        friendRoomBaseData.limitGoldMin = warehouseCfg.getEnterLimit();
        return friendRoomBaseData;
    }


    /**
     * 获取房间剩余时间
     */
    public static long getRoomResetTime(FriendRoom friendRoom) {
        if (friendRoom.getPauseTime() == 0) {
            return Math.max(0, friendRoom.getOverdueTime() - System.currentTimeMillis());
        }
        return Math.max(0, friendRoom.getOverdueTime() - friendRoom.getPauseTime());
    }
}
