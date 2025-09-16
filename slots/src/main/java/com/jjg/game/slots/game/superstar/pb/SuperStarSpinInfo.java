package com.jjg.game.slots.game.superstar.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 超级明星
 */
@ProtobufMessage
@ProtoDesc("游戏结果")
public class SuperStarSpinInfo {

    @ProtoDesc("图标id列表")
    public List<Integer> iconList;

    @ProtoDesc("中奖信息")
    public List<SuperStarResultLineInfo> resultLineInfoList;

    @ProtoDesc("jackpotId")
    public int jackpotId;

    @ProtoDesc("获得的奖池金额")
    public long jackpotValue;

    public List<Integer> getIconList() {
        return iconList;
    }

    public void setIconList(List<Integer> iconList) {
        this.iconList = iconList;
    }

    public List<SuperStarResultLineInfo> getResultLineInfoList() {
        return resultLineInfoList;
    }

    public void setResultLineInfoList(List<SuperStarResultLineInfo> resultLineInfoList) {
        this.resultLineInfoList = resultLineInfoList;
    }

    public long getJackpotValue() {
        return jackpotValue;
    }

    public void setJackpotValue(long jackpotValue) {
        this.jackpotValue = jackpotValue;
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
