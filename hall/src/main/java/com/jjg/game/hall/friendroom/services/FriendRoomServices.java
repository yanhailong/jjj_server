package com.jjg.game.hall.friendroom.services;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.IllegalNameCheckService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.friendroom.dao.RoomFriendDao;
import com.jjg.game.hall.friendroom.message.req.ReqCreateFriendsRoom;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.RoomExpendCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * 好友房服务
 *
 * @author 2CL
 */
@Service
public class FriendRoomServices {

    @Autowired
    private RoomFriendDao friendDao;
    @Autowired
    private IllegalNameCheckService illegalNameCheckService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private HallRoomDao hallRoomDao;

    /**
     * 创建好友房
     */
    public void createFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {

    }

    /**
     * 检查是否能创建房间
     */
    private int checkCreateRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        // 房间名检查
        if (!illegalNameCheckService.illegalNameCheck(reqCreateFriendsRoom.roomAliasName)) {
            return Code.ILLEGAL_NAME;
        }
        // 检查场次是否存在
        RoomCfg roomCfg = GameDataManager.getRoomCfg(reqCreateFriendsRoom.roomCfgId);
        if (roomCfg != null) {
            return Code.PARAM_ERROR;
        }
        // 牌局时长合法性检查
        List<RoomExpendCfg> roomExpendCfgs = GameDataManager.getRoomExpendCfgList();
        RoomExpendCfg roomExpendCfg = roomExpendCfgs.stream()
            .filter(cfg -> cfg.getDurationtype() == 1 && cfg.getDurationTime() == reqCreateFriendsRoom.timeOfOpenRoom)
            .findAny()
            .orElse(null);
        if (roomExpendCfg == null) {
            return Code.PARAM_ERROR;
        }
        int moneyId = roomExpendCfg.getRequiredMoney().isEmpty() ? 0 : roomExpendCfg.getRequiredMoney().getFirst();
        int itemId = roomExpendCfg.getRequiredItem().isEmpty() ? 0 : roomExpendCfg.getRequiredItem().getFirst();
        // 检查道具ID合法性
        if (moneyId != reqCreateFriendsRoom.itemId && itemId != reqCreateFriendsRoom.itemId) {
            return Code.PARAM_ERROR;
        }
        // 房间数量检查
        // 准备金扣费检查
        CommonResult<PlayerPack> removeResult = playerPackService.removeItem(
            playerController.playerId(), reqCreateFriendsRoom.itemId, reqCreateFriendsRoom.itemNum);
        if (!removeResult.success()) {
            return removeResult.code;
        }
        return Code.SUCCESS;
    }
}
