package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.FriendRoom;

/**
 * 大厅和房间之间的通信接口
 *
 * @author 2CL
 */
public interface HallRoomBridge extends IGameRpc {

    /**
     * 在节点中，创建一个好友房，空房间，但是需要走时间
     *
     * @param roomId 房间ID
     * @return true 创建成功
     */
    boolean createFriendRoom(int roomCfgId, long roomId);

    /**
     * 操作好友房
     *
     * @param playerId        玩家ID
     * @param roomId          房间ID
     * @param operateCode 操作码 1. 重新开启 2. 暂停 3. 解散
     */
    void operateFriendRoom(long playerId, long roomId, int operateCode, int roomCfgId);


    /**
     * 好友房自动续费
     *
     * @param roomId    房间ID
     * @param roomCfgId 配置id
     * @param gameType 游戏类型
     */
    CommonResult<FriendRoom> friendRoomAutoRenewal(long roomId, int roomCfgId, int gameType);

    /**
     * 获取好友房信息
     *
     * @param roomId 房间ID
     * @return 好友房
     */
    FriendRoom getFriendRoomInfo(long roomId);

    /**
     * 更新房间信息
     *
     * @param playerId
     * @param roomId
     * @param addTime            增加时长
     * @param autoRenewal
     * @param predictCostGoldNum 添加的庄家准备金
     * @param roomAliasName
     */
    CommonResult<FriendRoom> updateFriendRoom(long playerId, int roomCfgId, long roomId, int addTime, boolean autoRenewal, long predictCostGoldNum, String roomAliasName);
}
