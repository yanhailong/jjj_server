package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.ploy.games.mining.MiningConstant;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_STATE, resp = true)
@ProtoDesc("挖矿状态或操作结果")
public class ResMiningState extends AbstractResponse {
    @ProtoDesc("操作类型，0刷新")
    public int action;
    @ProtoDesc("失败原因，如STALE_VERSION/CELL_NOT_CONNECTED")
    public String reason;
    @ProtoDesc("最新状态；失败时可为空，版本过期请刷新")
    public MiningInfo info;
    @ProtoDesc("本次实际获得的奖励")
    public List<ItemInfo> rewards;
    @ProtoDesc("本次受击且滚动后仍在可视区域内的格子")
    public List<MiningCellInfo> changed;
    @ProtoDesc("本次地图向上滚动的行数，0表示未滚动")
    public int scrollRows;
    @ProtoDesc("本次被挖开且产出奖励的格子及各自奖励，使用操作前的绝对坐标")
    public List<MiningRewardCellInfo> rewardCells;
    public ResMiningState(int code) { super(code); }
}
