package com.jjg.game.hall.minigame.game.luckytreasure.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.hall.minigame.game.luckytreasure.constant.LuckyTreasureConstant;
import com.jjg.game.hall.minigame.game.luckytreasure.message.req.ReqBuyLuckyTreasure;
import com.jjg.game.hall.minigame.game.luckytreasure.message.req.ReqLuckyTreasureInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.message.req.ReqReceiveLuckyTreasure;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.ResBuyLuckyTreasure;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.ResLuckyTreasureInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.ResReceiveLuckyTreasure;
import com.jjg.game.hall.minigame.game.luckytreasure.service.LuckyTreasureService;
import org.springframework.stereotype.Component;

/**
 * 夺宝奇兵协议入口
 */
@Component
@MessageType(MessageConst.MessageTypeDef.MINIGAME)
public class LuckyTreasureMessageHandler {



}
