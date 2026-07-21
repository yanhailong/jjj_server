package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.poker.game.common.gamephase.BasePokerPhase;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianRecharge;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * 即时充值复活阶段，DESIGN.md 8.9：结算后金币归零的玩家进入30s复活倒计时。
 * <p>
 * 花钻石换金币的兑换逻辑见 {@link DouXianGameController#reqRecharge}(阶段13已实现，是纯内存的
 * 货币兑换，跟真实支付网关无关)。倒计时到了还没充值成功的玩家，一律按认输处理(见
 * {@link DouXianGameController#forceFinishRechargePhase})，保证房间流程不会因为一直等一个
 * 钻石不够或者不想充值的玩家而卡死。
 */
public class DouXianRechargePhase extends BasePokerPhase<DouXianGameDataVo> {

    public DouXianRechargePhase(AbstractPhaseGameController<Room_ChessCfg, DouXianGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.RECHARGE;
    }

    @Override
    public int getPhaseRunTime() {
        return DouXianConstant.Time.RECHARGE_TIME;
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        long overTime = System.currentTimeMillis() + DouXianConstant.Time.RECHARGE_TIME;
        DouXianDataHelper.DouXianRechargeCost cost = DouXianDataHelper.getRechargeCost();
        for (Long playerId : gameDataVo.getRechargingPlayerIds()) {
            NotifyDouXianRecharge notify = new NotifyDouXianRecharge();
            notify.playerId = playerId;
            notify.state = 1;
            notify.overTime = overTime;
            if (cost != null) {
                notify.diamondCost = cost.diamondCost();
                notify.goldReward = cost.goldReward();
            }
            broadcastMsgToRoom(notify);
        }
        log.info("斗仙牌进入即时充值复活等待 roomCfgId:{} playerIds:{}",
                gameDataVo.getRoomCfg().getId(), gameDataVo.getRechargingPlayerIds());
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof DouXianGameController controller) {
            controller.forceFinishRechargePhase();
        }
    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {
    }
}
