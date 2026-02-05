package com.jjg.game.slots.game.basketballSuperstar.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 篮球巨星房间模式玩家数据
 * @author lm
 * @date 2025/8/5 16:14
 */
@Document(collection = "BasketballSuperstarPlayerGameDataRoomDTO")
public class BasketballSuperstarPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
}
