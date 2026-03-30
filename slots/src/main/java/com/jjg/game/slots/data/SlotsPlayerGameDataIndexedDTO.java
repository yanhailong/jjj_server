package com.jjg.game.slots.data;

import org.springframework.data.mongodb.core.index.CompoundIndex;

/**
 * 带默认复合索引的玩家数据基类
 * @author lm
 * @date 2026/2/4
 */
@CompoundIndex(name = "playerId_roomcfgid_unique_idx", def = "{'playerId': 1, 'roomCfgId': 1}", unique = true)
public class SlotsPlayerGameDataIndexedDTO extends SlotsPlayerGameDataDTO {
}
