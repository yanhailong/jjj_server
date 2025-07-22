package com.jjg.game.table.redblackwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.res.BetTableInfo;
import com.jjg.game.table.common.message.res.RespTablePlayerInfo;
import com.jjg.game.table.common.message.res.TablePlayerInfo;
import com.jjg.game.table.redblackwar.message.RedBlackWarMessageConstant;
import com.jjg.game.table.redblackwar.message.bean.RedBlackWarHistory;

import java.util.List;

/**
 * 通知百家乐当前场上信息，玩家第一次进入初始化界面信息
 *
 * @author lm
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE,
        cmd = RedBlackWarMessageConstant.RespMsgBean.NOTIFY_RED_BLACK_WAR_INFO,
        resp = true
)
@ProtoDesc("通知红黑大战桌上信息")
public class NotifyRedBlackWarInfo extends AbstractMessage {

    @ProtoDesc("场上阶段信息")
    public EGamePhase gamePhase;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

    @ProtoDesc("区域下注信息")
    public List<BetTableInfo> tableAreaInfos;

    @ProtoDesc("玩家当前信息")
    public List<TablePlayerInfo> playerInfos;

    @ProtoDesc("当前房间的历史记录")
    public List<RedBlackWarHistory> redBlackHistories;

    @ProtoDesc("结算信息")
    public NotifyRedBlackWarSettleInfo settleInfos;
}
