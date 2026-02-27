package com.jjg.game.slots.game.hulk.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.hulk.HulkConstant;
import com.jjg.game.slots.game.hulk.dao.HulkGameDataDao;
import com.jjg.game.slots.game.hulk.dao.HulkResultLibDao;
import com.jjg.game.slots.game.hulk.data.*;
import com.jjg.game.slots.game.hulk.pb.HulkWinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2026/1/15
 */
public abstract class AbstractHulkGameManager extends AbstractSlotsGameManager<HulkPlayerGameData, HulkResultLib, HulkGameRunInfo> {
    @Autowired
    protected HulkResultLibDao libDao;
    @Autowired
    protected HulkGenerateManager generateManager;
    @Autowired
    protected HulkGameDataDao gameDataDao;

    public AbstractHulkGameManager() {
        super(HulkPlayerGameData.class, HulkResultLib.class, HulkGameRunInfo.class);
    }

    @Override
    public HulkGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        HulkPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new HulkGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }


    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param stake
     * @return
     */
    @Override
    protected HulkGameRunInfo startGame(PlayerController playerController, HulkPlayerGameData playerGameData, long stake, boolean auto) {
        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerGameData.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == HulkConstant.Status.NORMAL) {  //正常
                gameRunInfo = normal(gameRunInfo, playerGameData, stake);
            } else if (status == HulkConstant.Status.FREE) { //免费模式
                gameRunInfo = free(gameRunInfo, playerGameData, HulkConstant.SpecialMode.FREE);
            } else if (status == HulkConstant.Status.ONE_WILD) {  //第3列wild
                gameRunInfo = free(gameRunInfo, playerGameData, HulkConstant.SpecialMode.ONT_WILD);
            } else if (status == HulkConstant.Status.THREE_WILD) {  //第234列wild
                gameRunInfo = free(gameRunInfo, playerGameData, HulkConstant.SpecialMode.THREE_WILD);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 免费模式
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected HulkGameRunInfo free(HulkGameRunInfo gameRunInfo, HulkPlayerGameData playerGameData, int libType) {
        CommonResult<HulkResultLib> libResult = freeGetLib(playerGameData, libType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        HulkResultLib freeGame = libResult.data;

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(HulkConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            //最后一局，通知客户端，累计免费模式的中奖金额
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);

            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {},toLibType = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), libType);
        }

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore(), true));
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setResultLib(freeGame);

        return gameRunInfo;
    }

    @Override
    protected HulkGameRunInfo normal(HulkGameRunInfo gameRunInfo, HulkPlayerGameData playerGameData, long betValue, HulkResultLib resultLib) {
        //是否触发特殊模式
        int libType = resultLib.getLibTypeSet().stream().findFirst().get().intValue();

        int clientShowStatus = HulkConstant.SpecialMode.NORMAL;
        if (libType == HulkConstant.SpecialMode.NORMAL) {
            //因为normal概率最大，所以放在开头
        } else if (libType == HulkConstant.SpecialMode.FREE) {
            clientShowStatus = HulkConstant.Status.FREE;
            playerGameData.setStatus(HulkConstant.Status.FREE);
            playerGameData.setFreeLib(resultLib);
            log.debug("触发免费  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        } else if (libType == HulkConstant.SpecialMode.MINI) {
            clientShowStatus = HulkConstant.Status.MINI;
            log.debug("触发小游戏  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        } else if (libType == HulkConstant.SpecialMode.ONT_WILD) {
            clientShowStatus = HulkConstant.Status.ONE_WILD;
            playerGameData.setStatus(HulkConstant.Status.ONE_WILD);
            playerGameData.setFreeLib(resultLib);
            log.debug("第3列变成wild  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        } else if (libType == HulkConstant.SpecialMode.THREE_WILD) {
            clientShowStatus = HulkConstant.Status.THREE_WILD;
            playerGameData.setStatus(HulkConstant.Status.THREE_WILD);
            playerGameData.setFreeLib(resultLib);
            log.debug("第234列变成wild  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        gameRunInfo.setIconArr(resultLib.getIconArr());

        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore(), false));
        gameRunInfo.setStatus(clientShowStatus);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
        return gameRunInfo;
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<HulkWinIconInfo> transAwardLinePbInfo(List<HulkAwardLineInfo> infoList, long oneBetScore, boolean freeModel) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        List<HulkWinIconInfo> list = new ArrayList<>(infoList.size());
        for (HulkAwardLineInfo lineInfo : infoList) {
            HulkWinIconInfo resultLineInfo = new HulkWinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId(), freeModel).subList(0, lineInfo.getSameCount());
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    @Override
    protected HulkResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected HulkGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected HulkGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return HulkPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.HULK;
    }
}
