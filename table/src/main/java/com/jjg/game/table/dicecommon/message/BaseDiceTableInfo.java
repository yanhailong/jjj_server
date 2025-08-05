package com.jjg.game.table.dicecommon.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.List;

/**
 * 基础骰子桌面信息
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("骰子类基础桌面信息")
public class BaseDiceTableInfo {

    @ProtoDesc("场上阶段信息")
    public EGamePhase gamePhase;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

    @ProtoDesc("区域下注信息")
    public List<BetTableInfo> tableAreaInfos;

    @ProtoDesc("押分分值列表")
    public List<Integer> betPointList;

    @ProtoDesc("桌面上金币前7的玩家数据")
    public List<TablePlayerInfo> playerInfo;

    @ProtoDesc("房间总人数")
    public int totalPlayerNum;
}
