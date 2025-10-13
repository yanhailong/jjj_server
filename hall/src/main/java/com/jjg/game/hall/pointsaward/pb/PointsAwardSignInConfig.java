package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 积分大奖签到每天信息
 */
@ProtobufMessage
@ProtoDesc("积分大奖签到每天信息")
public class PointsAwardSignInConfig {

    /**
     * 对应每月的几号
     */
    @ProtoDesc("对应每月的几号")
    private int dayOfMonth;

    /**
     * 奖励的道具
     */
    @ProtoDesc("奖励的道具")
    private List<ItemInfo> itemList;

    /**
     * 奖励的积分
     */
    @ProtoDesc("奖励的积分")
    private int integralNum;

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
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
