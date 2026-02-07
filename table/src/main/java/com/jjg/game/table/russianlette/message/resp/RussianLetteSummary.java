package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 房间摘要，进入场次时展示
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘房间摘要")
public class RussianLetteSummary {

    @ProtoDesc("roomId")
    public long roomId;

    @ProtoDesc("俄罗斯转盘游戏基础信息")
    public RussianLetteBaseInfo baseInfo;

    @ProtoDesc("结果转盘状态")
    public List<Integer> cardStateList;

    @ProtoDesc("场次ID")
    public int wareId;

    @ProtoDesc("红色概率")
    public double red;

    @ProtoDesc("黑色概率")
    public double black;

    @ProtoDesc("奇数概率")
    public double odd;

    @ProtoDesc("偶数概率")
    public double event;


}
