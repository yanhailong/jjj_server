package com.jjg.game.common.pb;


import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage
@ProtoDesc("网络状态枚举")
public enum NetStatEnum {
    @ProtoDesc("网络不可用")
    NETWORK_CANT_USE(0, "网络不可用"),
    @ProtoDesc("连接成功")
    NETWORK_SUCCESS(1, "连接成功"),
    @ProtoDesc("服务不可用")
    SERVICE_CANT_USE(2, "服务不可用"),
    @ProtoDesc("踢出下线")
    PLAYER_KICKOUT(3, "踢出下线");


    public int code;
    public String message;

    NetStatEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
