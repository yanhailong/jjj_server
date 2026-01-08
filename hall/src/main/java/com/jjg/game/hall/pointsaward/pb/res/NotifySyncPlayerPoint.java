package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 通知同步玩家积分
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = PointsAwardConstant.Message.SYNC_POINT, resp = true)
@ProtoDesc("通知同步玩家积分")
public class NotifySyncPlayerPoint extends AbstractNotice {

    /**
     * 当前积分
     */
    @ProtoDesc("当前积分")
    private long point;

    /**
     * 默认返回月榜排名
     */
    @ProtoDesc("默认返回月榜排名 -1未入榜")
    private int rank;

    /**
     * 更新状态 1=请求 2=主推
     */
    @ProtoDesc("更新状态 1=请求 2=主推")
    private int state;

    @ProtoDesc("url")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
