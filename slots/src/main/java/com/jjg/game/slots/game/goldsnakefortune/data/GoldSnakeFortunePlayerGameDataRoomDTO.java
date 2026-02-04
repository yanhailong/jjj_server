package com.jjg.game.slots.game.goldsnakefortune.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * GoldSnakeFortune房间模式玩家数据
 * @author lm
 * @date 2026/2/4
 */
@Document(collection = "GoldSnakeFortunePlayerGameDataRoomDTO")
public class GoldSnakeFortunePlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
}
