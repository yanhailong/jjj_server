package com.jjg.game.hall.friendroom.services;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.hall.friendroom.dao.RoomFriendDao;
import com.jjg.game.hall.friendroom.message.req.ReqCreateFriendsRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 好友房服务
 *
 * @author 2CL
 */
@Service
public class FriendRoomServices {

    @Autowired
    private RoomFriendDao friendDao;

    /**
     * 创建好友房
     */
    public void createFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {

    }

    /**
     * 检查是否能创建房间
     */
    private int checkCreateRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        // 检查场次是否存在
        // 牌局时长
        return Code.SUCCESS;
    }
}
