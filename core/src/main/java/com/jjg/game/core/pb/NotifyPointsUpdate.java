package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 通知玩家积分变化
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE,
        cmd = MessageConst.ToServer.NOTIFY_PLAYER_POINTS_UPDATE, resp = true, toPbFile = false)
@ProtoDesc("通知玩家积分变化")
public class NotifyPointsUpdate extends AbstractMessage {

    /**
     * 玩家id
     */
    @ProtoDesc("玩家id")
    private long playerId;

    /**
     * 变化类型 true 增加 false扣除
     */
    @ProtoDesc("变化类型 true 增加 false扣除")
    private boolean flag;

    /**
     * 变化的值
     */
    @ProtoDesc("变化的值")
    private int value;

    /**
     * 获取途径
     */
    @ProtoDesc("获取途径")
    private int type;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
