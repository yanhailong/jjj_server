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
 * @date 2025/12/2 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ZEUS_VS_HADES, cmd = ZeusVsHadesConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResZeusVsHadesEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常   1.二选一  2.jackPool  3.宙斯   4.哈里斯")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("奖池信息")
    public List<ZeusVsHadesPoolInfo> poolList;
    @ProtoDesc("宙斯免费次数（小游戏）")
    public int zeusFree;
    @ProtoDesc("中奖变成大图标列 对应倍数（从1列开始 key是列 value是倍数）")
    public List<KVInfo> wildColumnTimes;
    @ProtoDesc("哈迪斯wild的位置")
    public Set<Integer> hadesWildSet;
    @ProtoDesc("wild状态 1宙斯赢 2哈迪斯赢 （从1列开始 key是列 value是倍数）")
    public List<KVInfo> wildColumnStatus;

    public ResZeusVsHadesEnterGame(int code) {
        super(code);
    }
}
