package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 加载积分大奖排行历史记录
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_LOAD_LEADERBOARD_HISTORY
)
@ProtoDesc("加载积分大奖排行历史记录")
public class ReqLoadLeaderboardHistory extends AbstractMessage {

    /**
     * 当前请求的分页索引，用于标识当前加载的排行榜页码。
     * 通常值从0开始，代表第一页，以此类推。
     */
    @ProtoDesc("页码")
    private int pageIndex;

    /**
     * 每页加载的记录数量。
     * 用于分页加载排行榜数据时指定每页的记录数目。
     */
    @ProtoDesc("每页数量")
    private int pageSize;

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
