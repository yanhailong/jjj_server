package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;

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
     * 加载条数
     */
    @ProtoDesc("加载条数")
    private int count;

    /**
     * 排行数据
     */
    @ProtoDesc("排行数据")
    private PointsAwardLeaderboardData rankingData;

    /**
     * 玩家自己的名次 -1未上榜
     */
    @ProtoDesc("玩家自己的名次  -1未上榜")
    private int selfIndex;

    public ResLoadLeaderboard(int code) {
        super(code);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public PointsAwardLeaderboardData getRankingData() {
        return rankingData;
    }

    public void setRankingData(PointsAwardLeaderboardData rankingData) {
        this.rankingData = rankingData;
    }

    public int getSelfIndex() {
        return selfIndex;
    }

    public void setSelfIndex(int selfIndex) {
        this.selfIndex = selfIndex;
    }
}
