package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求转盘旋转回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_TURNTABLE,
        resp = true
)
@ProtoDesc("请求转盘旋转回复")
public class ResPointAwardTurntableSpin extends AbstractResponse {

    /**
     * 中奖id
     */
    @ProtoDesc("中奖id")
    private int gridId;

    public ResPointAwardTurntableSpin(int code) {
        super(code);
    }

    public int getGridId() {
        return gridId;
    }

    public void setGridId(int gridId) {
        this.gridId = gridId;
    }
}
