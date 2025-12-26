package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜信息
 */
@ProtobufMessage
@ProtoDesc("排行榜信息")
public class PointsAwardLeaderboardData {

    /**
     * 排行榜类型 1=日榜 2=周榜 3=月榜
     */
    @ProtoDesc("排行榜类型 1=日榜 2=周榜 3=月榜")
    private int rankType;

    /**
     * 结束时间 用于计算排行榜名字 未结束的排行榜为当前时间戳
     */
    @ProtoDesc("结束时间 用于计算排行榜名字 未结束的排行榜为当前时间戳")
    private long endTime;

    /**
     * 整个排行榜信息
     */
    @ProtoDesc("整个排行榜信息")
    private List<PointsAwardLeaderboardInfo> rankingInfoList = new ArrayList<>();

    public int getRankType() {
        return rankType;
    }

    public void setRankType(int rankType) {
        this.rankType = rankType;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<PointsAwardLeaderboardInfo> getRankingInfoList() {
        return rankingInfoList;
    }

    public void setRankingInfoList(List<PointsAwardLeaderboardInfo> rankingInfoList) {
        this.rankingInfoList = rankingInfoList;
    }

}
