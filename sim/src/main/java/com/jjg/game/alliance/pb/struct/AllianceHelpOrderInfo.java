package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 互助求助订单项 (联盟频道卡片/求助列表用)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("互助求助订单")
public class AllianceHelpOrderInfo {
    @ProtoDesc("订单id")
    public long orderId;
    @ProtoDesc("类型 1任务求助 2建筑加速")
    public int type;
    @ProtoDesc("求助者id")
    public long ownerId;
    @ProtoDesc("求助者昵称")
    public String ownerNick;
    @ProtoDesc("目标id (任务uid/建筑id)")
    public long targetId;
    @ProtoDesc("目标展示名 (任务名/建筑名)")
    public String targetName;
    @ProtoDesc("已获帮助次数")
    public int helped;
    @ProtoDesc("可获帮助次数上限")
    public int maxHelp;
    @ProtoDesc("发起时间(ms)")
    public long createTime;
    @ProtoDesc("我是否已帮助过该单")
    public boolean myHelped;
    @ProtoDesc("结束时间(ms): 建筑加速为建筑升级 CD 结束时间")
    public long endTime;
}
