package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.ploy.games.mining.MiningConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.REQ_INFO)
@ProtoDesc("进入或刷新挖矿")
public class ReqMiningInfo extends AbstractMessage { }
