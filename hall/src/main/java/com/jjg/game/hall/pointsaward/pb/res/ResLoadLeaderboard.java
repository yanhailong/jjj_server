package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求加载排行数据回返
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_LOAD_LEADERBOARD,
        resp = true
)
@ProtoDesc("请求加载排行数据回返")
public class ResLoadLeaderboard extends AbstractResponse {

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

    /**
     * 最大页码
     */
    @ProtoDesc("最大页码")
    private int maxPageIndex;

    /**
     * 最大条数
     */
    @ProtoDesc("最大条数")
    private int totalCount;

    /**
     * 排行数据
     */
    @ProtoDesc("排行数据")
    private List<PointsAwardLeaderboardData> dataList = new ArrayList<>();

    /**
     * 玩家自己的名次 -1未上榜
     */
    @ProtoDesc("玩家自己的名次  -1未上榜")
    private int selfIndex;

    /**
     * 玩家自己的积分
     */
    @ProtoDesc("玩家自己的积分")
    private int selfPoint;

    public ResLoadLeaderboard(int code) {
        super(code);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSelfIndex() {
        return selfIndex;
    }

    public void setSelfIndex(int selfIndex) {
        this.selfIndex = selfIndex;
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

    public int getMaxPageIndex() {
        return maxPageIndex;
    }

    public void setMaxPageIndex(int maxPageIndex) {
        this.maxPageIndex = maxPageIndex;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<PointsAwardLeaderboardData> getDataList() {
        return dataList;
    }

    public void setDataList(List<PointsAwardLeaderboardData> dataList) {
        this.dataList = dataList;
    }

    public int getSelfPoint() {
        return selfPoint;
    }

    public void setSelfPoint(int selfPoint) {
        this.selfPoint = selfPoint;
    }
}
