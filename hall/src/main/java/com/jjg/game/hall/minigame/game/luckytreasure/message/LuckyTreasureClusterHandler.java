package com.jjg.game.hall.minigame.game.luckytreasure.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.pb.LuckyTreasureUpdateBroadcast;
import com.jjg.game.hall.minigame.game.luckytreasure.service.LuckyTreasureService;
import org.springframework.stereotype.Component;

/**
 * 夺宝奇兵其他节点同步库存消息处理器
 */
@Component
@MessageType(MessageConst.MessageTypeDef.HALL_TYPE)
public class LuckyTreasureClusterHandler {

    private final LuckyTreasureService luckyTreasureService;

    public LuckyTreasureClusterHandler(LuckyTreasureService luckyTreasureService) {
        this.luckyTreasureService = luckyTreasureService;
    }

    /**
     * 收到其他节点同步更新库存数据
     */
    @Command(MessageConst.ToServer.NOTIFY_LUCKY_TREASURE_UPDATE_STOCK)
    public void handleLuckyTreasureUpdate(LuckyTreasureUpdateBroadcast message) {
        luckyTreasureService.handleUpdateMessage(message.getIssueNumber());
    }

}
