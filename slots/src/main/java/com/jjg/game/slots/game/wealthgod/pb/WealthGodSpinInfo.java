package com.jjg.game.slots.game.wealthgod.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 财神旋转数据
 */
@ProtobufMessage
@ProtoDesc("游戏结果")
public class WealthGodSpinInfo {

    @ProtoDesc("图标id列表")
    public List<Integer> iconList;

    /**
     * 总的中奖倍率
     */
    @ProtoDesc("总的中奖倍率")
    public int times;

    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;

    @ProtoDesc("中奖信息")
    public List<WealthGodResultLineInfo> resultLineInfoList;

    @ProtoDesc("图标变化信息列表")
    public List<WealthGodIconChangeInfo> iconChangeInfoList;

    public List<Integer> getIconList() {
        return iconList;
    }

    public void setIconList(List<Integer> iconList) {
        this.iconList = iconList;
    }

    public List<WealthGodResultLineInfo> getResultLineInfoList() {
        return resultLineInfoList;
    }

    public void setResultLineInfoList(List<WealthGodResultLineInfo> resultLineInfoList) {
        this.resultLineInfoList = resultLineInfoList;
    }

    public List<WealthGodIconChangeInfo> getIconChangeInfoList() {
        return iconChangeInfoList;
    }

    public void setIconChangeInfoList(List<WealthGodIconChangeInfo> iconChangeInfoList) {
        this.iconChangeInfoList = iconChangeInfoList;
    }

    public long getBigWinShow() {
        return bigWinShow;
    }

    public void setBigWinShow(long bigWinShow) {
        this.bigWinShow = bigWinShow;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }
}
