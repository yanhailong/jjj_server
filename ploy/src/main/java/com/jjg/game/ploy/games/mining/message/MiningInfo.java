package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.ItemInfo;
import java.util.List;

@ProtobufMessage
@ProtoDesc("挖矿完整可视状态")
public class MiningInfo {
    @ProtoDesc("赛季ID")
    public String seasonId;
    @ProtoDesc("赛季结束时间毫秒，0常驻")
    public long seasonEndTime;
    @ProtoDesc("存档版本，操作必须原样传回")
    public long version;
    @ProtoDesc("列数")
    public int width;
    @ProtoDesc("可视行数")
    public int visibleRows;
    @ProtoDesc("顶部绝对行")
    public int topRow;
    @ProtoDesc("最深挖开行")
    public int depth;
    @ProtoDesc("每日刷新时间毫秒")
    public long nextDailyReset;
    @ProtoDesc("仅可视区域，后续预生成内容不下发")
    public List<MiningCellInfo> cells;
    @ProtoDesc("挖矿工具余额")
    public List<ItemInfo> tools;
    @ProtoDesc("矿石余额")
    public List<ItemInfo> ores;
    @ProtoDesc("工具已耗尽，显示结算/获取入口，存档保留")
    public boolean toolsExhausted;
    @ProtoDesc("累计挖开格子")
    public long totalGrids;
    @ProtoDesc("累计矿石基础价值")
    public long resourceValue;
    @ProtoDesc("非空表示特殊奖励需核账，禁止重复操作")
    public String pendingDeliveryId;
}
