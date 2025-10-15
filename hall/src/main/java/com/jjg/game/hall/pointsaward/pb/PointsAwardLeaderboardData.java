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
     * 排行榜类型 1=上午榜 2=下午榜 3=月榜
     */
    @ProtoDesc("排行榜类型 1=上午榜 2=下午榜 3=月榜")
    private int rankType;

    /**
     * 排行榜名字
     */
    @ProtoDesc("排行榜名字")
    private String name;

    /**
     * 结束时间
     */
    @ProtoDesc("结束时间")
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
