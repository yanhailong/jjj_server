package com.jjg.game.table.baccarat.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.NotifyBaccaratBetStart;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.bean.ReqBetBean;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 下注
 *
 * @author 2CL
 */
public class BaccaratTableBetPhase extends BaseTableBetPhase<BaccaratGameDataVo> {

    private static final Logger log = LoggerFactory.getLogger(BaccaratTableBetPhase.class);

    public BaccaratTableBetPhase(AbstractGameController<Room_BetCfg, BaccaratGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 向玩家通知场上数据,发送 BET_START 的阶段数据
        NotifyBaccaratBetStart baccaratTableInfo =
            BaccaratMessageBuilder.buildNotifyBaccaratBetStart(gameDataVo);
        broadcastMsgToRoom(baccaratTableInfo);
        // 通知所有观察者
        BaccaratMessageBuilder.notifyObserversOnPhaseChange((BaccaratGameController) gameController);
    }

    @Override
    public void phaseFinish() {
    }


    @Override
    public void dealBet(PlayerController playerController, ReqBet reqBet) {
        List<ReqBetBean> reqBetBeans = reqBet.reqBetBeans;
        NotifyPlayerBet notifyPlayerBet = new NotifyPlayerBet(Code.SUCCESS);
        notifyPlayerBet.playerId = playerController.playerId();
        if (reqBetBeans == null || reqBetBeans.isEmpty()) {
            notifyPlayerBet.code = Code.FAIL;
            playerController.send(notifyPlayerBet);
            return;
        }
        // 判断合法性
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
        // 检查是否是合法押注
        int checkRes = checkBetAction(gamePlayer, reqBetBeans);
        log.info("玩家：{} 请求下注，下注数据：{}", playerController.playerId(), JSON.toJSONString(reqBet));
        if (checkRes != Code.SUCCESS) {
            notifyPlayerBet.code = checkRes;
            playerController.send(notifyPlayerBet);
            return;
        }
        long playerTotalBetGold = 0;
        notifyPlayerBet.betTableInfoList = new ArrayList<>();
        // 处理下注数据
        Map<Integer, List<Integer>> playerAreaInfoMap = gameDataVo.getPlayerBetInfo(playerController.playerId());
        for (ReqBetBean reqBetBean : reqBetBeans) {
            BetTableInfo betTableInfo = new BetTableInfo();
            int betAreaIdx = reqBetBean.betAreaIdx;
            long betValue = reqBetBean.betValue;
            playerTotalBetGold += betValue;
            if (playerAreaInfoMap == null) {
                playerAreaInfoMap = new HashMap<>();
                gameDataVo.updatePlayerBetInfo(playerController.playerId(), playerAreaInfoMap);
            }
            playerAreaInfoMap.computeIfAbsent(betAreaIdx, k -> new ArrayList<>()).add((int) betValue);
            betTableInfo.betIdx = betAreaIdx;
            betTableInfo.playerBetTotal = playerAreaInfoMap.get(betAreaIdx).stream().mapToInt(Integer::intValue).sum();
            betTableInfo.betIdxTotal = gameDataVo.getAreaTotalBet(betAreaIdx);
            // 返回下注响应消息
            betTableInfo.betValue = betValue;
            notifyPlayerBet.betTableInfoList.add(betTableInfo);
        }
        // 更新押注数据
        gameDataVo.updatePlayerBetInfo(playerController.playerId(), playerAreaInfoMap);
        // TODO 扣除玩家金币
        gamePlayer.setGold(gamePlayer.getGold() - playerTotalBetGold);
        notifyPlayerBet.playerCurGold = gamePlayer.getGold();
        // 向房间广播下注改变信息
        broadcastMsgToRoom(notifyPlayerBet);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
