package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2026/1/15
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document(collection = "HulkPlayerGameDataRoomDTO")
public class HulkPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
}
