package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求加载排行数据
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_LOAD_LEADERBOARD
)
@ProtoDesc("请求加载排行数据")
public class ReqLoadLeaderboard extends AbstractMessage {

    /**
     * 排行榜类型 配置表
     * 1、上午排行
     * 2、下午排行
     * 3、月总行榜
     */
    @ProtoDesc("排行榜类型 配置表的type字段 1、上午排行 2、下午排行 3、月总行榜")
    private int type;

    /**
     * 当前页码
     */
    @ProtoDesc("当前页码")
    private int pageIndex;

    /**
     * 每页条数
     */
    @ProtoDesc("每页条数")
    private int pageSize;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
