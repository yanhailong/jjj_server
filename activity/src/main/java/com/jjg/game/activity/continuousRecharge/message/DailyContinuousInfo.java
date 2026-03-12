package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/5
 */
@ProtobufMessage
@ProtoDesc("连续充值信息")
public class DailyContinuousInfo {
    @ProtoDesc("第几天，从0开始")
    public int index;
    @ProtoDesc("进度信息")
    public List<ContinuousProgressInfo> progressInfoList;
    @ProtoDesc("今日充值")
    public String todayValue;
}
