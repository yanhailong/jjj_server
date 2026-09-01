package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.mining.MiningConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.REQ_EXCHANGE_SHOP)
@ProtoDesc("请求挖矿兑换商店")
public class ReqMiningExchangeShop extends AbstractMessage { }
