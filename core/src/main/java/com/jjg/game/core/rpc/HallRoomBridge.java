package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.FriendRoom;

/**
 * 大厅和房间之间的通信接口
 *
 * @author 2CL
 */
public interface HallRoomBridge extends IGameRpc {

    /**
     * 操作好友房
     *
     * @param roomId      房间ID
     * @param operateCode 操作码 1. 暂停 2. 重新开启 3. 解散
     */
    void operateFriendRoom(long roomId, int operateCode);

    /**
     * 获取好友房信息
     *
     * @param roomId 房间ID
     * @return 好友房
     */
    FriendRoom getFriendRoomInfo(long roomId);
}
