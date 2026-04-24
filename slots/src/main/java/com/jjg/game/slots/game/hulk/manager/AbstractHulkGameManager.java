package com.jjg.game.slots.game.hulk.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryAwardInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.hulk.HulkConstant;
import com.jjg.game.slots.game.hulk.dao.HulkResultLibDao;
import com.jjg.game.slots.game.hulk.data.HulkAwardLineInfo;
import com.jjg.game.slots.game.hulk.data.HulkGameRunInfo;
import com.jjg.game.slots.game.hulk.data.HulkPlayerGameData;
import com.jjg.game.slots.game.hulk.data.HulkResultLib;
import com.jjg.game.slots.game.hulk.pb.HulkWinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2026/1/15
 */
public abstract class AbstractHulkGameManager extends AbstractSlotsGameManager<HulkPlayerGameData, HulkResultLib, HulkGameRunInfo> {
    @Autowired
    protected HulkResultLibDao libDao;
    @Autowired
    protected HulkGenerateManager generateManager;

    public AbstractHulkGameManager() {
        super(HulkPlayerGameData.class, HulkResultLib.class, HulkGameRunInfo.class);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public HulkGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        HulkPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new HulkGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        resetFreeStateIfInvalid(playerGameData, HulkConstant.Status.FREE, HulkConstant.Status.NORMAL, "hulk");
        resetFreeStateIfInvalid(playerGameData, HulkConstant.Status.ONE_WILD, HulkConstant.Status.NORMAL, "hulk");
        resetFreeStateIfInvalid(playerGameData, HulkConstant.Status.THREE_WILD, HulkConstant.Status.NORMAL, "hulk");
        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
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
        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerGameData.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
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
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.getPlayerId());
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
     * 汽车小游戏
     *
     * @param playerController
     * @param carIndex
     * @return
     */
    public HulkGameRunInfo miniGameCar(PlayerController playerController, int carIndex) {
        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            //获取玩家游戏数据
            HulkPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("获取玩家游戏数据失败，汽车小游戏失败 playerId = {},gameType = {},roomCfgId = {},carIndex = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status != HulkConstant.Status.TRIGGER_MINI) {
                log.debug("当前不处于小游戏状态，汽车小游戏失败 playerId = {},gameType = {},roomCfgId = {},carIndex = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取缓存的结果库
            if (playerGameData.getFreeLib() == null) {
                playerGameData.setStatus(0);
                log.debug("未获取到结果库，汽车小游戏失败，且恢复状态 playerId = {},gameType = {},roomCfgId = {},carIndex = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            int libType = playerGameData.getFreeLib().getLibTypeSet().stream().findFirst().get().intValue();
            if (libType != HulkConstant.SpecialMode.MINI) {
                playerGameData.setStatus(0);
                log.debug("缓存的结果库libType错误，汽车小游戏失败，且恢复状态 playerId = {},gameType = {},roomCfgId = {},carIndex = {},libType = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex, libType);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取结果库中的小游戏信息
            SpecialAuxiliaryAwardInfo miniGameInfo = getMiniGameInfo(playerGameData.getFreeLib());
            if (miniGameInfo == null || miniGameInfo.getAwardCList() == null || miniGameInfo.getAwardCList().isEmpty()) {
                playerGameData.setStatus(0);
                log.debug("获取结果库中的小游戏信息失败，汽车小游戏失败，且恢复状态 playerId = {},gameType = {},roomCfgId = {},carIndex = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            Long gold = playerGameData.carByIndex(carIndex);
            if(gold != null){
                log.debug("之前已经摧毁过该汽车，汽车小游戏失败 playerId = {},gameType = {},roomCfgId = {},carIndex = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //检查汽车游戏是否结束
            int hasSize = playerGameData.carSize();
            int diff = miniGameInfo.getAwardCList().size() - hasSize;
            if (diff < 1) {
                log.debug("当前汽车游戏已经结束，汽车小游戏失败 playerId = {},gameType = {},roomCfgId = {},carIndex = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), carIndex);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            int times = miniGameInfo.getAwardCList().get(hasSize);
            gold = playerGameData.getOneBetScore() * times;
            playerGameData.addCarInfo(hasSize, gold);
            gameRunInfo.setAllWinGold(gold);
            if (diff < 2) {
                playerGameData.setCarOver(true);
                gameRunInfo.setCarOver(true);
            }

            log.info("汽车小游戏中奖金额 playerId = {},index = {},gold = {},carOver = {}", playerController.playerId(), carIndex, gold, gameRunInfo.isCarOver());
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 飞机小游戏
     *
     * @param playerController
     * @return
     */
    public HulkGameRunInfo miniGameAirPlane(PlayerController playerController) {
        HulkGameRunInfo gameRunInfo = new HulkGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            //获取玩家游戏数据
            HulkPlayerGameData playerGameData = getPlayerGameData(playerController);
            if (playerGameData == null) {
                log.debug("获取玩家游戏数据失败，飞机小游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status != HulkConstant.Status.TRIGGER_MINI) {
                log.debug("当前不处于小游戏状态，飞机小游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取缓存的结果库
            if (playerGameData.getFreeLib() == null) {
                playerGameData.setStatus(0);
                log.debug("未获取到结果库，飞机小游戏失败，且恢复状态 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            int libType = playerGameData.getFreeLib().getLibTypeSet().stream().findFirst().get().intValue();
            if (libType != HulkConstant.SpecialMode.MINI) {
                playerGameData.setStatus(0);
                log.debug("缓存的结果库libType错误，飞机小游戏失败，且恢复状态 playerId = {},gameType = {},roomCfgId = {},libType = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), libType);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //获取结果库中的小游戏信息
            SpecialAuxiliaryAwardInfo miniGameInfo = getMiniGameInfo(playerGameData.getFreeLib());
            if (miniGameInfo == null || miniGameInfo.getAwardCList() == null || miniGameInfo.getAwardCList().isEmpty()) {
                playerGameData.setStatus(0);
                log.debug("获取结果库中的小游戏信息失败，飞机小游戏失败，且恢复状态 playerId = {},gameType = {},roomCfgId = {},", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //检查飞机游戏是否结束
            int hasSize = playerGameData.carSize();
            if (hasSize < 1) {
                log.debug("还没有进行汽车游戏，飞机小游戏失败 playerId = {},gameType = {},roomCfgId = {},hasSize = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), hasSize);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            long allGold = 0;
            for (Map.Entry<Integer, Long> en : playerGameData.getCarMap().entrySet()) {
                allGold += en.getValue();
            }

            long addGold = allGold * miniGameInfo.getAwardD();
            rewardFromBigPool(gameRunInfo, playerGameData, addGold, AddType.SLOTS_BET_REWARD);
            if (gameRunInfo.getCode() != Code.SUCCESS) {
                log.info("飞机小游戏失败 playerId = {},carAllGold = {},times = {},calAddGold={},code = {}", playerController.playerId(), allGold, miniGameInfo.getAwardD(), addGold, gameRunInfo.getCode());
                return gameRunInfo;
            }
            gameRunInfo.setAirplane(miniGameInfo.getAwardD());

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerGameData.getPlayer().getRoomCfgId());
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, playerGameData.getPlayer()));
            playerGameData.setCarOver(false);
            playerGameData.setStatus(0);
            playerGameData.setFreeLib(null);
            playerGameData.setCarMap(null);
            log.info("飞机小游戏结束 playerId = {},carAllGold = {},times = {},calAddGold={},realAddGold = {}", playerController.playerId(), allGold, miniGameInfo.getAwardD(), addGold, gameRunInfo.getAllWinGold());
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

            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {},toLibType = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId(), libType);
        }

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore(), true));
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setResultLib(freeGame);

        return gameRunInfo;
    }

    /**
     * 获取结果库中的小游戏信息
     *
     * @param resultLib
     * @return
     */
    public SpecialAuxiliaryAwardInfo getMiniGameInfo(HulkResultLib resultLib) {
        for (SpecialAuxiliaryInfo info : resultLib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryCfg cfg = GameDataManager.getSpecialAuxiliaryCfg(info.getCfgId());
            if (cfg.getType() != HulkConstant.SpecialAuxiliary.MINI_GAME) {
                continue;
            }
            if (info.getAwardInfos() == null || info.getAwardInfos().isEmpty()) {
                continue;
            }
            for (SpecialAuxiliaryAwardInfo awardInfo : info.getAwardInfos()) {
                if (awardInfo.getAwardCList() == null || awardInfo.getAwardCList().isEmpty()) {
                    continue;
                }

                return awardInfo;
            }
        }
        return null;
    }


    @Override
    protected HulkGameRunInfo normal(HulkGameRunInfo gameRunInfo, HulkPlayerGameData playerGameData, long betValue, HulkResultLib resultLib) {
        //是否触发特殊模式
        int libType = resultLib.getLibTypeSet().stream().findFirst().get().intValue();

        long times = resultLib.getTimes();
        int clientShowStatus = HulkConstant.Status.NORMAL;
        if (libType == HulkConstant.SpecialMode.NORMAL) {
            //因为normal概率最大，所以放在开头
        } else if (libType == HulkConstant.SpecialMode.FREE) {
            clientShowStatus = HulkConstant.Status.TRIGGER_FREE;
            playerGameData.setStatus(HulkConstant.Status.FREE);
            playerGameData.setFreeLib(resultLib);
            playerGameData.setRemainFreeCount(new AtomicInteger(resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames().size()));
            times = resultLib.getTriggerTimes();
            log.debug("触发免费  playerId = {},libId = {},status = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus());
        } else if (libType == HulkConstant.SpecialMode.MINI) {
            clientShowStatus = HulkConstant.Status.TRIGGER_MINI;
            playerGameData.setStatus(HulkConstant.Status.TRIGGER_MINI);
            playerGameData.setFreeLib(resultLib);
            times = resultLib.getTriggerTimes();
            log.debug("触发小游戏  playerId = {},libId = {},status = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus());
        } else if (libType == HulkConstant.SpecialMode.ONT_WILD) {
            clientShowStatus = HulkConstant.Status.TRIGGER_ONE_WILD;
            playerGameData.setStatus(HulkConstant.Status.ONE_WILD);
            playerGameData.setFreeLib(resultLib);
            playerGameData.setRemainFreeCount(new AtomicInteger(resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames().size()));
            times = resultLib.getTriggerTimes();
            log.debug("第3列变成wild  playerId = {},libId = {},status = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus());
        } else if (libType == HulkConstant.SpecialMode.THREE_WILD) {
            clientShowStatus = HulkConstant.Status.TRIGGER_THREE_WILD;
            playerGameData.setStatus(HulkConstant.Status.THREE_WILD);
            playerGameData.setFreeLib(resultLib);
            playerGameData.setRemainFreeCount(new AtomicInteger(resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames().size()));
            times = resultLib.getTriggerTimes();
            log.debug("第234列变成wild  playerId = {},libId = {},status = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus());
        }

        gameRunInfo.setIconArr(resultLib.getIconArr());

        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(times);
        }

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore(), false));
        gameRunInfo.setStatus(clientShowStatus);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
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
    protected HulkGenerateManager getGenerateManager() {
        return this.generateManager;
    }


    @Override
    public int getGameType() {
        return CoreConst.GameType.HULK;
    }
}
