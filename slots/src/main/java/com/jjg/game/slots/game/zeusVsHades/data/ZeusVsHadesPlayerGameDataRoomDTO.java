package com.jjg.game.slots.game.zeusVsHades.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lihaocao
 * @date 2025/8/5 16:14
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document(collection = "ZeusVsHadesPlayerGameDataRoomDTO")
public class ZeusVsHadesPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
}
