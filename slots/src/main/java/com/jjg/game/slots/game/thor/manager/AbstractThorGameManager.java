package com.jjg.game.slots.game.thor.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.game.thor.dao.ThorGameDataDao;
import com.jjg.game.slots.game.thor.dao.ThorResultLibDao;
import com.jjg.game.slots.game.thor.data.*;
import com.jjg.game.slots.game.thor.pb.ThorWinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractThorGameManager extends AbstractSlotsGameManager<ThorPlayerGameData, ThorResultLib> {
    @Autowired
    protected ThorResultLibDao libDao;
    @Autowired
    protected ThorGenerateManager generateManager;
    @Autowired
    protected ThorGameDataDao gameDataDao;

    public AbstractThorGameManager() {
        super(ThorPlayerGameData.class, ThorResultLib.class);
    }

    @Override
    public ThorGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        ThorPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ThorGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        ThorGameRunInfo gameRunInfo = new ThorGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public ThorGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        ThorPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ThorGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 免费模式二选一
     *
     * @param playerController
     * @return
     */
    public ThorGameRunInfo freeChooseOne(PlayerController playerController, int chooseType) {
        //获取玩家游戏数据
        ThorPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ThorGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        if (playerGameData.getStatus() != ThorConstant.Status.CHOOSE_ONE) {
            log.debug("玩家当前不处于二选一状态，二选一失败 playerId = {},gameType = {},roomCfgId = {},status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), playerGameData.getStatus());
            return new ThorGameRunInfo(Code.FORBID, playerController.playerId());
        }

        if (chooseType == 0) {
            playerGameData.setStatus(ThorConstant.Status.FIRE);
        } else {
            playerGameData.setStatus(ThorConstant.Status.ICE);
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return new ThorGameRunInfo(Code.SUCCESS, playerController.playerId());
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param stake
     * @return
     */
    protected ThorGameRunInfo startGame(PlayerController playerController, ThorPlayerGameData playerGameData, long stake, boolean auto) {
        ThorGameRunInfo gameRunInfo = new ThorGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == ThorConstant.Status.NORMAL) {  //正常
                gameRunInfo = normal(gameRunInfo, playerGameData, stake);
            } else if (status == ThorConstant.Status.CHOOSE_ONE) {  //二选一
                gameRunInfo.setCode(Code.FORBID);
                log.debug("当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            } else if (status == ThorConstant.Status.FIRE) {  //火焰免费
                gameRunInfo = free(gameRunInfo, playerGameData, ThorConstant.SpecialMode.FIRE);
            } else if (status == ThorConstant.Status.ICE) {  //冰冻免费
                gameRunInfo = free(gameRunInfo, playerGameData, ThorConstant.SpecialMode.ICE);
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
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), stake, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, stake);
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
     * 普通正常流程
     *
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    protected ThorGameRunInfo normal(ThorGameRunInfo gameRunInfo, ThorPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<ThorResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, ThorConstant.SpecialMode.TYPE_NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        ThorResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(ThorConstant.SpecialMode.FREE)) {  //是否会触发二选一
            playerGameData.setStatus(ThorConstant.Status.CHOOSE_ONE);
            log.debug("触发二选一  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        gameRunInfo.setIconArr(resultLib.getIconArr());

        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotId(), false);

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore(), false));
        gameRunInfo.setStatus(playerGameData.getStatus());
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
        return gameRunInfo;
    }

    /**
     * 免费模式
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected ThorGameRunInfo free(ThorGameRunInfo gameRunInfo, ThorPlayerGameData playerGameData, int specialModeFreeLibType) {
        CommonResult<ThorResultLib> libResult = freeGetLib(playerGameData, specialModeFreeLibType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        ThorResultLib freeGame = libResult.data;

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(ThorConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            //最后一局，通知客户端，累计免费模式的中奖金额
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            gameRunInfo.setFreeEnd(true);

            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore(), true));
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setResultLib(freeGame);
        return gameRunInfo;
    }

    @Override
    protected CommonResult<ThorResultLib> freeGetLib(ThorPlayerGameData playerGameData, int specialModeFreeLibType, int specialAuxiliary) {
        CommonResult<ThorResultLib> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始获取免费结果库 playerId = {}", playerGameData.playerId());

        ThorResultLib freeLib = (ThorResultLib) playerGameData.getFreeLib();
        if (freeLib == null) {
            //缓存中没有，就从数据库获取
            CommonResult<ThorResultLib> libResult = getLibFromDB(playerGameData, specialModeFreeLibType);
            if(!libResult.success()) {
                result.code = libResult.code;
                return result;
            }

            freeLib = libResult.data;
        }

        if (freeLib == null) {
            log.warn("未在该条结果库中找到免费转信息 gameType = {},modelId = {}", this.gameType, playerGameData.getLastModelId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (freeLib.getSpecialAuxiliaryInfoList() == null || freeLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            log.warn("未在该条结果库中找到免费转信息1 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        log.debug("找到免费旋转的结果库 libId = {}", freeLib.getId());

        //找到结果库中免费游戏的结果
        SpecialAuxiliaryInfo specialAuxiliaryInfo = null;
        for (Object obj : freeLib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryInfo tmpInfo = (SpecialAuxiliaryInfo) obj;
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(tmpInfo.getCfgId());
            if (specialAuxiliary > 0 && specialAuxiliaryCfg.getType() != specialAuxiliary) {
                continue;
            }
            if (tmpInfo.getFreeGames() == null || tmpInfo.getFreeGames().isEmpty()) {
                continue;
            }
            specialAuxiliaryInfo = tmpInfo;
            break;
        }

        if (specialAuxiliaryInfo == null) {
            log.warn("未在该条结果库中找到免费转信息2 gameType = {},libId = {}", this.gameType, freeLib.getId());
            result.code = Code.NOT_FOUND;
            return result;
        }

        int index = playerGameData.getFreeIndex().getAndAdd(1);
        JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(index);
        log.debug("获取免费游戏的下标 index = {},allLen = {}", index, specialAuxiliaryInfo.getFreeGames().size());
        ThorResultLib freeGame = JSON.parseObject(jsonObject.toJSONString(), this.libClass);

        if (freeGame == null) {
            log.warn("未在该条结果库中找到免费转信息3 gameType = {},libId = {}", this.gameType, freeLib.getId());
            playerGameData.setFreeLib(null);
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (index < 1) {
            playerGameData.setRemainFreeCount(new AtomicInteger(specialAuxiliaryInfo.getFreeGames().size()));
        }

        //缓存获取到的freeLib
        playerGameData.setFreeLib(freeLib);
        result.data = freeGame;
        return result;
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<ThorWinIconInfo> transAwardLinePbInfo(List<ThorAwardLineInfo> infoList, long oneBetScore, boolean freeModel) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        List<ThorWinIconInfo> list = new ArrayList<>(infoList.size());
        for (ThorAwardLineInfo lineInfo : infoList) {
            ThorWinIconInfo resultLineInfo = new ThorWinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId(), freeModel).subList(0, lineInfo.getSameCount());
//            resultLineInfo.times = lineInfo.getBaseTimes();
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    @Override
    protected void onAutoExitAction(ThorPlayerGameData gameData, int eventId) {
        //TODO
    }

    @Override
    protected ThorResultLibDao getResultLibDao() {
        return libDao;
    }

    @Override
    protected ThorGameDataDao getGameDataDao() {
        return gameDataDao;
    }

    @Override
    protected ThorGenerateManager getGenerateManager() {
        return generateManager;
    }

    @Override
    protected Class<ThorPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return ThorPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.THOR;
    }
}
