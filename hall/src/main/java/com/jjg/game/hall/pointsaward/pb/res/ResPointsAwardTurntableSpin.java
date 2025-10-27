package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableHistory;

/**
 * 请求转盘旋转回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_TURNTABLE,
        resp = true
)
@ProtoDesc("请求转盘旋转回复")
public class ResPointsAwardTurntableSpin extends AbstractResponse {

    /**
     * 当前旋转次数
     */
    @ProtoDesc("当前旋转次数")
    private int count;

    /**
     * 最大旋转次数
     */
    @ProtoDesc("最大旋转次数")
    private int maxCount;

    /**
     * 中奖id
     */
    @ProtoDesc("中奖id")
    private int gridId;

    /**
     * 本次转盘的历史记录 只有成功才有
     */
    @ProtoDesc("本次转盘的历史记录 只有成功才有")
    private PointsAwardTurntableHistory history;

    public ResPointsAwardTurntableSpin(int code) {
        super(code);
    }

    public int getGridId() {
        return gridId;
    }

    public void setGridId(int gridId) {
        this.gridId = gridId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public PointsAwardTurntableHistory getHistory() {
        return history;
    }

    public void setHistory(PointsAwardTurntableHistory history) {
        this.history = history;
    }
}
