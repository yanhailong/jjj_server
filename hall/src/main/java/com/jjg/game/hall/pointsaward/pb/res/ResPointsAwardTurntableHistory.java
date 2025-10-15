package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求转盘历史记录
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_TURNTABLE_HISTORY,
        resp = true
)
@ProtoDesc("请求转盘历史记录")
public class ResPointsAwardTurntableHistory extends AbstractResponse {

    /**
     * 历史记录列表
     */
    @ProtoDesc("历史记录列表")
    private List<PointsAwardTurntableHistory> historyList = new ArrayList<>();

    public ResPointsAwardTurntableHistory(int code) {
        super(code);
    }

    public List<PointsAwardTurntableHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<PointsAwardTurntableHistory> historyList) {
        this.historyList = historyList;
    }
}
