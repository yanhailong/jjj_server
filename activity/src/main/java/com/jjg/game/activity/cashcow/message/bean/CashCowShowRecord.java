package com.jjg.game.activity.cashcow.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 摇钱树记录
 *
 * @author lm
 * @date 2025/9/9 17:19
 */
@ProtobufMessage
@ProtoDesc("摇钱树记录")
public class CashCowShowRecord {
    @ProtoDesc("期数")
    public long round;
    @ProtoDesc("时间")
    public long recordTime;
    @ProtoDesc("昵称")
    public String name;
    @ProtoDesc("类型")
    public int type;
    @ProtoDesc("获奖数量")
    public long num;
}
