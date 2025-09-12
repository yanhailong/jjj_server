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

    @ProtoDesc("中奖信息")
    public List<WealthGodResultLineInfo> resultLineInfoList;

    @ProtoDesc("图标变化信息列表")
    public List<WealthGodIconChangeInfo> iconChangeInfoList;

    @ProtoDesc("免费旋转数据")
    public WealthGodSpinInfo freeSpin;

    @ProtoDesc("获得的奖池金额")
    public long jackpotValue;

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

    public WealthGodSpinInfo getFreeSpin() {
        return freeSpin;
    }

    public void setFreeSpin(WealthGodSpinInfo freeSpin) {
        this.freeSpin = freeSpin;
    }
}
