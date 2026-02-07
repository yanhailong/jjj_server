package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.baccarat.message.resp.BaccaratBaseInfo;
import com.jjg.game.table.baccarat.message.resp.BaccaratCardState;

/**
 * 俄罗斯转盘房间摘要，进入场次时展示
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘房间摘要")
public class RussianLetteSingleRes {

    @ProtoDesc("俄罗斯转盘游戏基础信息")
    public RussianLetteBaseInfo baccaratBaseInfo;

    @ProtoDesc("俄罗斯转盘牌状态")
    public RussianLetteCardState baccaratCardState;

    @ProtoDesc("对局ID")
    public int roundId;

    @ProtoDesc("是否需要清除路单, 在结算阶段告知下一回合服务器是否会重新洗牌")
    public boolean needClearRoad;

    @ProtoDesc("红色概率")
    public double red;

    @ProtoDesc("黑色概率")
    public double black;

    @ProtoDesc("奇数概率")
    public double odd;

    @ProtoDesc("偶数概率")
    public double event;
}
