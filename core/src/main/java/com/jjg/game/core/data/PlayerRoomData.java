package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 保存玩家相关的数据，例如房间的连输，连赢，和一些需要长期存放与房间相关的数值数据
 *
 * @author 2CL
 */
@Document("player_room")
public class PlayerRoomData {
    @Id
    private int playerId;

}
