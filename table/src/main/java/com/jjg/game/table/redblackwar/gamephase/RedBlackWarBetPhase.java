package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.ReqBetBean;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;
import com.jjg.game.table.redblackwar.manager.RedBlackWarSampleManager;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 下注
 *
 * @author 2CL
 */
public class RedBlackWarBetPhase extends BaseTableBetPhase<RedBlackWarGameDataVo> {

    private final RedBlackWarSampleManager redBlackWarSampleManager;
    private final CorePlayerService corePlayerService;
    private final Logger log = LoggerFactory.getLogger(RedBlackWarBetPhase.class);

    public RedBlackWarBetPhase(AbstractGameController<Room_BetCfg, RedBlackWarGameDataVo> gameController) {
        super(gameController);
        redBlackWarSampleManager = CommonUtil.getContext().getBean(RedBlackWarSampleManager.class);
        corePlayerService = CommonUtil.getContext().getBean(CorePlayerService.class);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        broadcastMsgToRoom(TableMessageBuilder.getNotifyPhaseChangInfo(EGamePhase.BET, gameDataVo.getPhaseEndTime()));
    }

    @Override
    public void phaseFinish() {

    }

    @Override
    public void dealBet(PlayerController playerController, ReqBet reqBet) {
        List<ReqBetBean> betInfos = reqBet.reqBetBeans;
        GamePlayer gamePlayer = gameDataVo.getGamePlayerMap().get(playerController.playerId());
        if (Objects.isNull(gamePlayer)) {
            log.error("玩家信息不存在，playerId：{}", playerController.playerId());
            return;
        }
        long needTotal = 0;
        // 押注筹码列表
        List<Integer> betList = gameDataVo.getRoomCfg().getBetList();
        //全部押注信息
        Map<Integer, Map<Long, Long>> betInfoMap = getGameDataVo().getBetInfo();
        for (ReqBetBean betInfo : betInfos) {
            //押注筹码判断
            if (betInfo.betValue < 0 || !betList.contains((int) betInfo.betValue)) {
                log.error("下注筹码错误，betValue：{}", betInfo.betValue);
                return;
            }
            needTotal += betInfo.betValue;
            //押注区域判断
            BetAreaCfg betAreaCfg = redBlackWarSampleManager.getBetAreaMap().get(betInfo.betAreaIdx);
            if (Objects.isNull(betAreaCfg)) {
                log.error("下注区域错误，betAreaIdx：{}", betInfo.betAreaIdx);
                return;
            }
            //区域总下注判断
            Map<Long, Long> playerBet = betInfoMap.computeIfAbsent(betInfo.betAreaIdx, key -> new HashMap<>());
            long totalBet = playerBet.values().stream().mapToLong(Long::longValue).sum();
            int tbUpperLimit = betAreaCfg.getTbUpperLimit();
            if (totalBet + betInfo.betValue > tbUpperLimit) {
                log.error("已达区域总押注上限，player：{} area:{} betValue:{}", gamePlayer.getId(), betInfo.betAreaIdx, needTotal);
                return;
            }
            long oldBet = playerBet.getOrDefault(gamePlayer.getId(), 0L);
            //个人总押注判断
            int tbPlayerUpperLimit = betAreaCfg.getTbPlayerUpperLimit();
            if (oldBet + betInfo.betValue > tbPlayerUpperLimit) {
                log.error("已达区域个人押注上限，player：{} area:{} betValue:{}", gamePlayer.getId(), betInfo.betAreaIdx, betInfo.betValue);
                return;
            }
        }
        // 下注金币判断
        if (gamePlayer.getGold() < needTotal) {
            log.error("玩家金币不足，playerGold：{} need:{}", gamePlayer.getGold(), needTotal);
            return;
        }
        //押注 扣除金币
        CommonResult<Player> result = corePlayerService.addGold(gamePlayer.getId(), -needTotal, "红黑大战押注");
        if (result.code != Code.SUCCESS) {
            log.error("保存玩家金币失败，player：{} area:{} betValue:{}", gamePlayer.getId(), betInfos, needTotal);
            return;
        }
        gamePlayer.setGold(result.data.getGold());
        //增加押注信息
        List<BetTableInfo> betTableInfos = new ArrayList<>();
        for (ReqBetBean betInfo : betInfos) {
            Map<Long, Long> longLongMap = betInfoMap.get(betInfo.betAreaIdx);
            Long merge = longLongMap.merge(gamePlayer.getId(), betInfo.betValue, Long::sum);
            long totalBet = longLongMap.values().stream().mapToLong(Long::longValue).sum();
            BetTableInfo betTableInfo = new BetTableInfo();
            betTableInfo.betValue = betInfo.betValue;
            betTableInfo.betIdx = betInfo.betAreaIdx;
            betTableInfo.betIdxTotal = totalBet;
            betTableInfo.playerBetTotal = merge;
            betTableInfos.add(betTableInfo);
        }
        //更新玩家当前总押注
        gamePlayer.getTableGameData().addTotalBet(needTotal);
        //通知玩家金币变化，区域变化
        NotifyPlayerBet notifyBetChange = new NotifyPlayerBet(Code.SUCCESS);
        notifyBetChange.playerId = gamePlayer.getId();
        notifyBetChange.playerCurGold = gamePlayer.getGold();
        notifyBetChange.betTableInfoList = betTableInfos;
        gameController.sendMessage(RoomMessageBuilder.newBuilder()
                .setData(notifyBetChange));
    }


}
