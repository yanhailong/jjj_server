package com.jjg.game.core.match.data;

import com.jjg.game.common.constant.StrConstant;

/**
 * 匹配房间redis的key值
 *
 * @author 2CL
 */
public interface MatchDataRedisKey {
    /**
     * 匹配相关数据族前缀
     */
    String BASE_MATCH_PREFIX = "match::";

    /**
     * 在房间创建时写入，如果房间满足人数上限时删除此key中的房间ID，如果一个房间在开局之后玩家主动离开后房间还可以继续添加玩家，
     * 需要将房间ID重新加入到key中
     * score值：时间+roomId
     */
    String WAIT_ROOMS_KEY = BASE_MATCH_PREFIX + "WaitRoomIds";

    // 获取人数未满房间的redis key
    static String getWaitJoinRoomsKey(int gameType, int roomConfigId) {
        return WAIT_ROOMS_KEY + StrConstant.DOUBLE_COLON + gameType + StrConstant.DOUBLE_COLON + roomConfigId;
    }
}
