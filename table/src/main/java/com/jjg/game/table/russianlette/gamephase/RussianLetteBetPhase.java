package com.jjg.game.table.russianlette.gamephase;

import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.resp.RussianLetteProb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 俄罗斯转盘下注阶段（BET）
 * <p>
 * 持续 {@code stageTime[1]} 秒（默认 13s，倒计时 12→0）。
 * <ul>
 *   <li>继承 {@link BaseTableBetPhase} 负责通用下注逻辑及机器人押注</li>
 *   <li>额外广播 {@code NotifyRussianLettePhaseChangInfo(BET)} 携带近 12 局概率数据</li>
 * </ul>
 * </p>
 *
 * @author lhc
 */
public class RussianLetteBetPhase extends BaseTableBetPhase<RussianLetteGameDataVo> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    public RussianLetteBetPhase(AbstractPhaseGameController<Room_BetCfg, RussianLetteGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public int getPhaseRunTime() {
        List<Integer> stageTime = gameDataVo.getRoomCfg().getStageTime();
        if (stageTime.size() >= 3) {
            return stageTime.get(0);
        }
        return 0;
    }


    /**
     * 阶段开始：
     * 1. 调用父类逻辑（清除下注数据、广播通用 NotifyPhaseChangInfo(BET)）
     * 2. 额外广播俄罗斯转盘专属阶段变化通知（携带近 12 局概率信息，无开奖结果）
     */
    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        log.info("执行RussianLetteBetPhase（下注阶段）中phaseDoAction");
        // 根据历史记录计算近 12 局的红/黑/奇/偶概率
        RussianLetteProb prob = RussianLetteMessageBuilder.buildProb(gameDataVo.getWinAreaCfgIdHistory());
        // 广播下注阶段变化通知（settlementInfo 在下注阶段为 null）
        broadcastMsgToRoom(
                RussianLetteMessageBuilder.buildPhaseChangInfo(
                        EGamePhase.BET,
                        gameDataVo.getPhaseEndTime(),
                        prob,
                        null));
        // 通知所有观察者（房间列表页玩家）
        RussianLetteMessageBuilder.notifyObserversOnPhaseChange(
                (BaseTableGameController<RussianLetteGameDataVo>) gameController);
    }

    @Override
    public void phaseFinish() {
        // 下注阶段结束，无额外操作
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
