package com.jjg.game.slots.game.angrybirds.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsGameDataDao;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsPlayerGameDataDTO;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsResultLibDao;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsAwardLineInfo;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsGameRunInfo;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsPlayerGameData;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsResultLib;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsWinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractAngryBirdsGameManager extends AbstractSlotsGameManager<AngryBirdsPlayerGameData, AngryBirdsResultLib, AngryBirdsGameRunInfo> {
    protected final AngryBirdsGenerateManager gameGenerateManager;
    protected final AngryBirdsGameDataDao gameDataDao;
    protected final AngryBirdsResultLibDao angryBirdsResultLibDao;

    public AbstractAngryBirdsGameManager(AngryBirdsGenerateManager gameGenerateManager,
                                         AngryBirdsGameDataDao gameDataDao, AngryBirdsResultLibDao angryBirdsResultLibDao) {
        super(AngryBirdsPlayerGameData.class, AngryBirdsResultLib.class, AngryBirdsGameRunInfo.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.angryBirdsResultLibDao = angryBirdsResultLibDao;
    }


    @Override
    public void init() {
//        log.info("启动愤怒的小鸟游戏管理器...");
//        super.init();

    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<AngryBirdsWinIconInfo> transAwardLinePbInfo(List<AngryBirdsAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }
        List<AngryBirdsWinIconInfo> list = new ArrayList<>(infoList.size());
        for (AngryBirdsAwardLineInfo lineInfo : infoList) {
            AngryBirdsWinIconInfo resultLineInfo = new AngryBirdsWinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexes = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    /**
     * 玩家开始游戏
     *
     */
    @Override
    public AngryBirdsGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        AngryBirdsPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new AngryBirdsGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        if (getRoomType() != null) {
            int code = slotsRoomManager.checkCanPlay(this, playerController);
            if (code != Code.SUCCESS) {
                log.debug("该游戏无法继续 playerId = {},gameType = {},roomCfgId = {},code = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), code);
                return new AngryBirdsGameRunInfo(code, playerController.playerId());
            }
        }
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     */
    @Override
    public AngryBirdsGameRunInfo startGame(PlayerController playerController, AngryBirdsPlayerGameData playerGameData, long betValue, boolean auto) {
        AngryBirdsGameRunInfo gameRunInfo = new AngryBirdsGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));
            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == AngryBirdsConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == AngryBirdsConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
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
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected AngryBirdsGameRunInfo normal(AngryBirdsGameRunInfo gameRunInfo, AngryBirdsPlayerGameData playerGameData, long betValue, AngryBirdsResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(AngryBirdsConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(AngryBirdsConstant.Status.FREE);
            playerGameData.setFreeLib(resultLib);
            if (CollectionUtil.isNotEmpty(resultLib.getSpecialAuxiliaryInfoList())) {
                for (SpecialAuxiliaryInfo auxiliaryInfo : resultLib.getSpecialAuxiliaryInfoList()) {
                    if (CollectionUtil.isNotEmpty(auxiliaryInfo.getFreeGames())) {
                        playerGameData.getRemainFreeCount().set(auxiliaryInfo.getFreeGames().size());
                    }
                }
            }
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(),
                    playerGameData.getRemainFreeCount().get());
        }
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());
        gameRunInfo.setReplaceInfo(resultLib.getReplaceInfoList());
        log.debug("id = {}", resultLib.getId());
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(playerGameData.getStatus());
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     */
    protected void free(AngryBirdsGameRunInfo gameRunInfo, AngryBirdsPlayerGameData playerGameData) {
        CommonResult<AngryBirdsResultLib> libResult = freeGetLib(playerGameData, AngryBirdsConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        AngryBirdsResultLib freeGame = libResult.data;
        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setReplaceInfo(freeGame.getReplaceInfoList());
        gameRunInfo.setFreeMultiplier(freeGame.getFreeMultiplier());
        if (afterCount == 0) {
            playerGameData.setStatus(AngryBirdsConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(playerGameData.getStatus());
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.ANGRY_BIRDS;
    }

    @Override
    protected AngryBirdsResultLibDao getResultLibDao() {
        return this.angryBirdsResultLibDao;
    }

    @Override
    protected AngryBirdsGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected AngryBirdsGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return AngryBirdsPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭愤怒的小鸟游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
