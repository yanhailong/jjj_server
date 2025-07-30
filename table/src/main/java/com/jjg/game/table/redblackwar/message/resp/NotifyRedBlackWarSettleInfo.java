package com.jjg.game.table.redblackwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;
import com.jjg.game.table.redblackwar.message.RedBlackWarMessageConstant;

import java.util.List;

/**
 * @author lm
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE,
        cmd = RedBlackWarMessageConstant.RespMsgBean.NOTIFY_RED_BLACK_WAR_SETTLE_INFO,
        resp = true)
@ProtoDesc("红黑大战结算信息")
public class NotifyRedBlackWarSettleInfo extends AbstractNotice {
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

    @ProtoDesc("前6玩家信息")
    public List<TablePlayerInfo> playerInfos;

    @ProtoDesc("玩家结算信息")
    public List<PlayerChangedGold> playerSettleInfos;

    @ProtoDesc("幸运一击")
    public boolean isLucky;
}

