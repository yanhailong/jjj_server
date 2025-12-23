package com.jjg.game.slots.game.wealthgod.manager;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;
import com.jjg.game.slots.game.wealthgod.data.WealthGodGameRunInfo;
import com.jjg.game.slots.game.wealthgod.data.WealthGodPlayerGameData;
import com.jjg.game.slots.game.wealthgod.data.WealthGodResultLib;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodSpinInfo;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WealthGodRoomGameManager extends AbstractWealthGodGameManager{
    public WealthGodRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public WealthGodGameRunInfo startGame(PlayerController playerController, WealthGodPlayerGameData playerGameData, long betValue) {
        WealthGodGameRunInfo gameRunInfo = new WealthGodGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            CommonResult<Pair<WealthGodResultLib, BetDivideInfo>> commonResult = normalGetLib(playerGameData, betValue, WealthGodConstant.SpecialMode.TYPE_NORMAL);
            if (!commonResult.success()) {
                gameRunInfo.setCode(commonResult.code);
                return gameRunInfo;
            }

            WealthGodResultLib resultLib = commonResult.data.getFirst();
            gameRunInfo.setBetDivideInfo(commonResult.data.getSecond());

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getDiamond());

            gameRunInfo.setStake(betValue);
            //所有的spin数据
            List<WealthGodSpinInfo> infoList = new ArrayList<>();
            respinAnalysis(resultLib, playerGameData.getOneBetScore(), infoList, betValue, gameRunInfo);
            gameRunInfo.setSpinInfo(infoList);
            //记录奖池id
            gameRunInfo.setJackpotId(resultLib.getJackpotId());

            //房间配置id
            int roomCfgId = player.getRoomCfgId();
            long addGold = 0;
            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(), player.getRoomId(), addGold, AddType.SLOTS_BET_REWARD);
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                }
            }
            int jackpotId = gameRunInfo.getJackpotId();
            //检测奖池奖励
            if (jackpotId > 0) {
                long pool = calculatePool(roomCfgId, jackpotId, playerController);
                if (pool > 0) {
                    addGold += pool;
                    roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(), player.getRoomId(), pool, AddType.SLOTS_JACKPOT_REWARD);
                    //记录发奖金额
                    gameRunInfo.setJackpotValue(pool);
                    //记录发奖后剩余的奖池金额
                    gameRunInfo.setPoolValue(getPoolValueByRoomCfgId(roomCfgId));
                }
            }
            if (addGold > 0) {
                gameRunInfo.addAllWinGold(addGold);
            }

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(),gameRunInfo.getAllWinGold(),betValue);

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getDiamond());

            gameRunInfo.setResultLib(resultLib);
            checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    protected PoolCfg randWinPool(WealthGodPlayerGameData playerGameData, int poolId) {
        return null;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
