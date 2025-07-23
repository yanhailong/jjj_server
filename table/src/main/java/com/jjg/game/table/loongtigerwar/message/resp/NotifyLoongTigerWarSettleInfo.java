package com.jjg.game.table.loongtigerwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.common.message.bean.PlayerSettleInfo;
import com.jjg.game.table.loongtigerwar.message.LoongTigerWarMessageConstant;

import java.util.List;

/**
 * @author lm
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LOONG_TIGER_WAR_TYPE,
        cmd = LoongTigerWarMessageConstant.RespMsgBean.NOTIFY_LOONG_TIGER_WAR_SETTLE_INFO,
        resp = true)
@ProtoDesc("龙虎斗结算信息")
public class NotifyLoongTigerWarSettleInfo extends AbstractNotice {

    @ProtoDesc("龙方牌")
    public int loongCard;

    @ProtoDesc("虎方牌")
    public int tigerCard;

    @ProtoDesc("获胜状态(1龙胜 2虎胜 3和)")
    public int winState;

    @ProtoDesc("玩家结算信息")
    public List<PlayerSettleInfo> playerSettleInfos;

}
