package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 转盘配置
 */
@ProtobufMessage
@ProtoDesc("转盘配置")
public class PointsAwardTurntableConfig {

    /**
     * 格子id
     */
    @ProtoDesc("格子id")
    private int gridId;

    /**
     * 道具奖励
     */
    @ProtoDesc("道具奖励")
    private List<ItemInfo> itemList;

    /**
     * 积分奖励
     */
    @ProtoDesc("积分奖励")
    private int integralNum;

    public int getGridId() {
        return gridId;
    }

    public void setGridId(int gridId) {
        this.gridId = gridId;
    }

    public List<ItemInfo> getItemList() {
        return itemList;
    }

    public void setItemList(List<ItemInfo> itemList) {
        this.itemList = itemList;
    }

    public int getIntegralNum() {
        return integralNum;
    }

    public void setIntegralNum(int integralNum) {
        this.integralNum = integralNum;
    }
}
