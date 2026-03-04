package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 俄罗斯转盘房间摘要，进入场次时展示
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘房间摘要")
public class RussianLetteSingleRes {

    @ProtoDesc("俄罗斯转盘游戏基础信息")
    public RussianLetteBaseInfo baseInfo;

    @ProtoDesc("转盘结果 只记录最新12把的数字（0-36）")
    public List<Integer> cardStateList;

    @ProtoDesc("对局ID")
    public int roundId;

    @ProtoDesc("是否需要清除路单, 在结算阶段告知下一回合服务器是否会重新洗牌")
    public boolean needClearRoad;

    @ProtoDesc("概率信息")
    public RussianLetteProb prob;
}
