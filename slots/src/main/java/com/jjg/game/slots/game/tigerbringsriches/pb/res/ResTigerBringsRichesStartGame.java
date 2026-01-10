package com.jjg.game.slots.game.tigerbringsriches.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;
import com.jjg.game.slots.game.tigerbringsriches.pb.bean.TigerBringsRichesWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TigerBringsRichesConstant.MsgBean.RES_TIGER_BRINGS_RICHES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResTigerBringsRichesStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("当前状态 0.正常 1.假福马模式 2福马")
    public int status;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  ")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public List<TigerBringsRichesWinIconInfo> winIconInfoList;
    @ProtoDesc("滚轴类型")
    public int scrollType;
    @ProtoDesc("特殊模式是否结束")
    public boolean isSpecialModeEnd;
    public ResTigerBringsRichesStartGame(int code) {
        super(code);
    }
}
