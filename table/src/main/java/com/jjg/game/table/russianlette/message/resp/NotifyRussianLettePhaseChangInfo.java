package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * 通知俄罗斯转盘阶段变化信息
 * <p>
 * 每次游戏阶段切换时广播此协议，客户端根据 gamePhase 做相应 UI 切换：
 * <ul>
 *   <li>REST → 展示休闲动画，倒计时结束后自动进入下注 UI</li>
 *   <li>BET → 开放下注区域，显示倒计时</li>
 *   <li>DRAW_ON → 关闭下注，播放轮盘旋转动画，settlementInfo 中含中奖号码</li>
 *   <li>GAME_ROUND_OVER_SETTLEMENT → 展示结算面板（详细结算数据走 NotifyRussianLetteSettlement）</li>
 * </ul>
 *
 * @author lm
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
        resp = true,
        cmd = RussianLetteMessageConstant.RespMsgBean.NOTIFY_RUSSIAN_LETTE_PHASE_CHANGE_INFO
)
@ProtoDesc("通知俄罗斯转盘阶段变化信息")
public class NotifyRussianLettePhaseChangInfo extends AbstractNotice {

    /** 当前游戏阶段：REST / BET / DRAW_ON / GAME_ROUND_OVER_SETTLEMENT */
    @ProtoDesc("当前阶段")
    public EGamePhase gamePhase;

    /** 本阶段结束时间戳（毫秒），客户端据此计算剩余倒计时 */
    @ProtoDesc("结束时间戳(ms)")
    public long endTime;

    /**
     * 近12局统计概率（下注、开奖阶段有值；休闲、结算阶段为 null）
     * red/black/odd/event 均为 0~1 之间的小数
     */
    @ProtoDesc("近12局统计概率信息")
    public RussianLetteProb prob;

    /**
     * 开奖结果（仅 DRAW_ON 阶段推送时有值，其余阶段为 null）
     * 包含：中奖区域 ID 列表（rewardAreaIdx）、转盘落点数字（diceData 0~36）
     */
    @ProtoDesc("开奖结果，仅 DRAW_ON 阶段有值")
    public RussianLetteSettlementInfo settlementInfo;
}
