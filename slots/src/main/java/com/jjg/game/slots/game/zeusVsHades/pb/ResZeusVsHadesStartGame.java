package com.jjg.game.slots.game.zeusVsHades.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;

import java.util.List;
import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ZEUS_VS_HADES, cmd = ZeusVsHadesConstant.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResZeusVsHadesStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("状态  0.普通   1.二选一  2.jackPool  3.宙斯   4.哈迪斯")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public List<ZeusVsHadesIconInfo> rewardIconInfo;
    @ProtoDesc("中奖变成大图标列 对应倍数（从1列开始 key是列 value是倍数）")
    public List<KVInfo> wildColumnTimes;
    @ProtoDesc("哈迪斯wild的位置")
    public Set<Integer> hadesWildSet;
    @ProtoDesc("wild状态 1宙斯赢 2哈迪斯赢")
    public int wildStatus;

    public ResZeusVsHadesStartGame(int code) {
        super(code);
    }
}
