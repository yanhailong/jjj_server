package com.jjg.game.table.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 请求押注的bean
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("请求押注的bean")
public class ReqBetBean {

    @ProtoDesc("请求下注的金额")
    public long betValue;

    @ProtoDesc("百家乐下注区域，1: 庄对 2: 和 3: 闲对 4: 闲 5: 庄 其他游戏走游戏区域配置ID")
    public int betAreaIdx;
}
