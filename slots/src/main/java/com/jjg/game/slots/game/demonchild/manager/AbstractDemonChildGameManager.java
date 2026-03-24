package com.jjg.game.slots.game.demonchild.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;
import com.jjg.game.slots.game.demonchild.dao.DemonChildGameDataDao;
import com.jjg.game.slots.game.demonchild.dao.DemonChildPlayerGameDataDTO;
import com.jjg.game.slots.game.demonchild.dao.DemonChildResultLibDao;
import com.jjg.game.slots.game.demonchild.data.DemonChildAwardLineInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildGameRunInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildPlayerGameData;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import com.jjg.game.slots.game.demonchild.pb.bean.DemonChildLineInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDemonChildGameManager extends AbstractSlotsGameManager<DemonChildPlayerGameData, DemonChildResultLib, DemonChildGameRunInfo> {
    protected final DemonChildGameGenerateManager gameGenerateManager;
    protected final DemonChildGameDataDao gameDataDao;
    protected final DemonChildResultLibDao demonChildResultLibDao;

    public AbstractDemonChildGameManager(DemonChildGameGenerateManager gameGenerateManager,
                                         DemonChildGameDataDao gameDataDao, DemonChildResultLibDao demonChildResultLibDao) {
        super(DemonChildPlayerGameData.class, DemonChildResultLib.class, DemonChildGameRunInfo.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.demonChildResultLibDao = demonChildResultLibDao;
    }


    @Override
    public void init() {
        log.info("启动恶魔之子游戏管理器...");
        super.init();

    }

    /**
     * 开始游戏
     *
     */
    @Override
    public DemonChildGameRunInfo startGame(PlayerController playerController, DemonChildPlayerGameData playerGameData, long betValue, boolean auto) {
        DemonChildGameRunInfo gameRunInfo = new DemonChildGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == DemonChildConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == DemonChildConstant.Status.FREE) {
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
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }


    /**
     * 普通正常流程
     *
     */
    @Override
    protected DemonChildGameRunInfo normal(DemonChildGameRunInfo gameRunInfo, DemonChildPlayerGameData playerGameData, long betValue, DemonChildResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(DemonChildConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(DemonChildConstant.Status.FREE);
            playerGameData.setFreeLib(resultLib);
            playerGameData.getRemainFreeCount().set(resultLib.getFreeTotalCount());
            gameRunInfo.setTotalFreeCount(resultLib.getFreeTotalCount());
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus(),
                    playerGameData.getRemainFreeCount().get(), resultLib.getTimes());
        }
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setStatus(playerGameData.getStatus());
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     */
    protected void free(DemonChildGameRunInfo gameRunInfo, DemonChildPlayerGameData playerGameData) {
        CommonResult<DemonChildResultLib> libResult = freeGetLib(playerGameData, DemonChildConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        DemonChildResultLib freeGame = libResult.data;
        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        if (afterCount == 0) {
            playerGameData.setStatus(DemonChildConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            playerGameData.setFreeAllWin(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setStatus(playerGameData.getStatus());
    }

    private List<DemonChildLineInfo> transAwardLinePbInfo(List<DemonChildAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }
        List<DemonChildLineInfo> list = new ArrayList<>(infoList.size());
        for (DemonChildAwardLineInfo lineInfo : infoList) {
            DemonChildLineInfo resultLineInfo = new DemonChildLineInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexes = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.DEMON_CHILD;
    }

    @Override
    protected DemonChildResultLibDao getResultLibDao() {
        return this.demonChildResultLibDao;
    }

    @Override
    protected DemonChildGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected DemonChildGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return DemonChildPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭Demon Child游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void onAutoExitAction(DemonChildPlayerGameData gameData, int eventId) {
//        //发放免费模式和探宝奖励
//        if (gameData.getStatus() == DemonChildConstant.Status.FREE) {
//            Object freeLib = gameData.getFreeLib();
//            if (freeLib instanceof DemonChildResultLib lib) {
//                List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = lib.getSpecialAuxiliaryInfoList();
//                int totalSize = 0;
//                for (SpecialAuxiliaryInfo auxiliaryInfo : specialAuxiliaryInfoList) {
//                    if (auxiliaryInfo.getFreeGames() != null) {
//                        totalSize = auxiliaryInfo.getFreeGames().size();
//                    }
//                }
//                int index = gameData.getFreeIndex().get();
//                for (int i = index; i < totalSize; i++) {
//                    startGame(new PlayerController(null, null), gameData, gameData.getAllBetScore(), true);
//                }
//            }
//        }
    }
}
