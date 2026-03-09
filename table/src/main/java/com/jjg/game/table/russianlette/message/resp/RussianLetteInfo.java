package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.List;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘桌上信息")
public class RussianLetteInfo {

    @ProtoDesc("房间的玩家信息")
    public List<TablePlayerInfo> tablePlayerInfoList;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

//    @ProtoDesc("场上总时间")
//    public int totalTime;

    @ProtoDesc("区域下注信息")
    public List<BetTableInfo> tableAreaInfos;
}
