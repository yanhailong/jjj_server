package com.jjg.game.table.redblackwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.redblackwar.message.RedBlackWarMessageConstant;
import com.jjg.game.table.redblackwar.message.bean.RBWPlayerSettleInfo;

import java.util.List;

/**
 * @author lm
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE,
        cmd = RedBlackWarMessageConstant.RespMsgBean.NOTIFY_RED_BLACK_WAR_SETTLE_INFO,
        resp = true)
@ProtoDesc("红黑大战结算信息")
public class NotifyRedBlackWarSettleInfo extends AbstractResponse {
    @ProtoDesc("红方牌")
    public List<Integer> redCards;
    @ProtoDesc("红方牌型")
    public int redCardType;

    @ProtoDesc("黑方牌")
    public List<Integer> blackCards;
    @ProtoDesc("黑方牌型")
    public int blackCardType;

    @ProtoDesc("获胜状态(1红方胜 2黑方胜)")
    public int winState;

    @ProtoDesc("玩家获得的金币数")
    public long getGold;

    @ProtoDesc("玩家结算信息")
    public List<RBWPlayerSettleInfo> playerSettleInfos;

    public NotifyRedBlackWarSettleInfo() {
        super(Code.SUCCESS);
    }
}
