package com.jjg.game.slots.game.superstar.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * SuperStar房间模式玩家数据
 * @author lm
 * @date 2026/2/4
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document
public class SuperStarPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
}
