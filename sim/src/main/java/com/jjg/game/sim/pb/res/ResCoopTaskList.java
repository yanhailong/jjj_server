package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.CoopTaskInfo;

import java.util.List;

/**
 * 多人任务今日列表返回。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COOP_TASK_LIST, resp = true)
@ProtoDesc("多人任务今日列表返回")
public class ResCoopTaskList extends AbstractResponse {
    @ProtoDesc("任务列表 (今日池 + 跨天保留的已领取任务)")
    public List<CoopTaskInfo> tasks;
    @ProtoDesc("今日剩余领取次数")
    public int remainClaimCount;
    @ProtoDesc("今日免费刷新是否可用")
    public boolean freeRefresh;
    @ProtoDesc("刷新消耗道具id (0=不可付费刷新)")
    public int refreshItemId;
    @ProtoDesc("刷新消耗道具数量")
    public long refreshItemCount;
    @ProtoDesc("下次任务列表刷新时间 (次日0点, ms)")
    public long nextRefreshTime;
    @ProtoDesc("每日最多领取任务次数")
    public int dailyClaimLimit;

    public ResCoopTaskList(int code) {
        super(code);
    }
}
