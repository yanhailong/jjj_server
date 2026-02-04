package com.jjg.game.slots.game.mahjiongwin.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MahjiongWin房间模式玩家数据
 * @author lm
 * @date 2026/2/4
 */
@Document(collection = "MahjiongWinPlayerGameDataRoomDTO")
public class MahjiongWinPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
}
