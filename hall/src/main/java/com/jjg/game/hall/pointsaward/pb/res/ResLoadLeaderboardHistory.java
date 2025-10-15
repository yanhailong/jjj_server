package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载积分大奖排行榜历史数据
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_LOAD_LEADERBOARD_HISTORY,
        resp = true
)
@ProtoDesc("加载积分大奖排行榜历史数据")
public class ResLoadLeaderboardHistory extends AbstractResponse {

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

    /**
     * 总页数
     */
    @ProtoDesc("总页数")
    private int totalPage;

    /**
     * 总条数
     */
    @ProtoDesc("总条数")
    private int totalCount;

    /**
     * 数据
     */
    @ProtoDesc("数据")
    private List<PointsAwardLeaderboardHistory> historyList = new ArrayList<>();

    public ResLoadLeaderboardHistory(int code) {
        super(code);
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

    public List<PointsAwardLeaderboardHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<PointsAwardLeaderboardHistory> historyList) {
        this.historyList = historyList;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
