package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.hall.minigame.game.mining.MiningConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.REQ_ACTION)
@ProtoDesc("挖矿操作")
public class ReqMiningAction extends AbstractMessage {
    @ProtoDesc("1挖掘 2兑换 3免费/广告礼包 4成就领奖 5每日任务领奖")
    public int action;
    @ProtoDesc("当前赛季ID")
    public String seasonId;
    @ProtoDesc("当前存档版本")
    public long version;
    @ProtoDesc("挖掘为工具配置ID101/102/103，其他操作为商品或任务ID")
    public int id;
    @ProtoDesc("挖掘绝对行")
    public int row;
    @ProtoDesc("挖掘列")
    public int column;
    @ProtoDesc("兑换份数，必须正数；其他操作填1")
    public int count;
    @ProtoDesc("广告平台服务端验证成功后签发的票据，不接受客户端观看完成标记")
    public String adTicket;
}
