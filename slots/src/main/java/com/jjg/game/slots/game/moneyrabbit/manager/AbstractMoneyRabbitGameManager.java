package com.jjg.game.slots.game.moneyrabbit.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialGirdCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneGameRunInfo;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortunePlayerGameData;
import com.jjg.game.slots.game.goldsnakefortune.pb.GoldSnakeFortuneCoinInfo;
import com.jjg.game.slots.game.moneyrabbit.MoneyRabbitConstant;
import com.jjg.game.slots.game.moneyrabbit.dao.MoneyRabbitGameDataDao;
import com.jjg.game.slots.game.moneyrabbit.dao.MoneyRabbitResultLibDao;
import com.jjg.game.slots.game.moneyrabbit.data.*;
import com.jjg.game.slots.game.moneyrabbit.pb.MoneyRabbitCoinInfo;
import com.jjg.game.slots.game.moneyrabbit.pb.MoneyRabbitWinIconInfo;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractMoneyRabbitGameManager extends AbstractSlotsGameManager<MoneyRabbitPlayerGameData, MoneyRabbitResultLib> {
    @Autowired
    protected MoneyRabbitResultLibDao libDao;
    @Autowired
    protected MoneyRabbitGenerateManager generateManager;
    @Autowired
    protected MoneyRabbitGameDataDao gameDataDao;

    //假免费的概率
    private int fake_free_prop = 0;

    public AbstractMoneyRabbitGameManager() {
        super(MoneyRabbitPlayerGameData.class, MoneyRabbitResultLib.class);
    }

    @Override
    public MoneyRabbitGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        MoneyRabbitPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new MoneyRabbitGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        MoneyRabbitGameRunInfo gameRunInfo = new MoneyRabbitGameRunInfo(Code.SUCCESS, playerGameData.playerId());
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
    public MoneyRabbitGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        MoneyRabbitPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new MoneyRabbitGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param stake
     * @return
     */
    protected MoneyRabbitGameRunInfo startGame(PlayerController playerController, MoneyRabbitPlayerGameData playerGameData, long stake, boolean auto) {
        MoneyRabbitGameRunInfo gameRunInfo = new MoneyRabbitGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerGameData.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == MoneyRabbitConstant.Status.NORMAL) {  //正常
                gameRunInfo = normal(gameRunInfo, playerGameData, stake);
            } else if (status == MoneyRabbitConstant.Status.REAL_FREE || status == MoneyRabbitConstant.Status.FREE) {  //免费
                gameRunInfo = free(gameRunInfo, playerGameData, MoneyRabbitConstant.SpecialMode.FREE);
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
    protected MoneyRabbitGameRunInfo normal(MoneyRabbitGameRunInfo gameRunInfo, MoneyRabbitPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<MoneyRabbitResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, MoneyRabbitConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        MoneyRabbitResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(MoneyRabbitConstant.SpecialMode.FREE)) {  //是否会触发二选一
            playerGameData.setRemainFreeCount(new AtomicInteger(8));
            playerGameData.setStatus(MoneyRabbitConstant.Status.FREE);
            gameRunInfo.setStatus(MoneyRabbitConstant.Status.REAL_FREE);
            log.debug("触发真免费  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        } else {
            //随机触发假免费
            if (SlotsUtil.calProp(this.fake_free_prop)) {
                gameRunInfo.setStatus(MoneyRabbitConstant.Status.FAKE_FREE);
                log.debug("触发假免费  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
            } else {
                gameRunInfo.setStatus(playerGameData.getStatus());
            }
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        gameRunInfo.setIconArr(resultLib.getIconArr());
        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        //设置金钱信息
        checkCoinInfo(gameRunInfo, playerGameData, resultLib);

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
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
    protected MoneyRabbitGameRunInfo free(MoneyRabbitGameRunInfo gameRunInfo, MoneyRabbitPlayerGameData playerGameData, int specialModeFreeLibType) {
        CommonResult<MoneyRabbitResultLib> libResult = freeGetLib(playerGameData, specialModeFreeLibType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        MoneyRabbitResultLib freeGame = libResult.data;

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());


        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(MoneyRabbitConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            playerGameData.setFreeAllWin(0);

            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        //设置金钱信息
        checkCoinInfo(gameRunInfo, playerGameData, freeGame);

        gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setResultLib(freeGame);
        return gameRunInfo;
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    protected List<MoneyRabbitWinIconInfo> transAwardLinePbInfo(List<MoneyRabbitAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        List<MoneyRabbitWinIconInfo> list = new ArrayList<>(infoList.size());
        for (MoneyRabbitAwardLineInfo lineInfo : infoList) {
            MoneyRabbitWinIconInfo resultLineInfo = new MoneyRabbitWinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
//            resultLineInfo.times = lineInfo.getBaseTimes();
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    /**
     * 设置设置金钱信息
     *
     * @return
     */
    protected void checkCoinInfo(MoneyRabbitGameRunInfo gameRunInfo, MoneyRabbitPlayerGameData playerGameData, MoneyRabbitResultLib lib) {
        if (lib.getSpecialGirdInfoList() == null || lib.getSpecialGirdInfoList().isEmpty()) {
            return;
        }

        List<MoneyRabbitCoinInfo> coinInfoList = null;
        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (specialGirdInfo.getValueMap() == null || specialGirdInfo.getValueMap().isEmpty()) {
                continue;
            }
            //检查修改的图标是否有美元图标
            SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(specialGirdInfo.getCfgId());
            if (!specialGirdCfg.getElement().containsKey(MoneyRabbitConstant.BaseElement.ID_COIN) && !specialGirdCfg.getElement().containsKey(MoneyRabbitConstant.BaseElement.ID_COIN2)) {
                continue;
            }

            for (Map.Entry<Integer, Integer> en : specialGirdInfo.getValueMap().entrySet()) {
                if (coinInfoList == null) {
                    coinInfoList = new ArrayList<>();
                }

                MoneyRabbitCoinInfo coinInfo = new MoneyRabbitCoinInfo();
                coinInfo.index = en.getKey();
                coinInfo.value = playerGameData.getOneBetScore() * en.getValue();
                coinInfoList.add(coinInfo);
//                log.debug("添加金钱信息 girdId = {},value = {}", coinInfo.index, coinInfo.value);
            }
        }
        gameRunInfo.setCoinInfoList(coinInfoList);
    }

    @Override
    protected void specialPlayConfig() {
        //随机触发假免费
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(MoneyRabbitConstant.SpecialPlay.ID_FAKE_FREE);
        if (specialPlayCfg == null || StringUtils.isBlank(specialPlayCfg.getValue())) {
            return;
        }

        this.fake_free_prop = Integer.parseInt(specialPlayCfg.getValue().split(",")[1]);
    }

    @Override
    protected void onAutoExitAction(MoneyRabbitPlayerGameData gameData, int eventId) {
        if (gameData.getStatus() == MoneyRabbitConstant.Status.FREE) {
            freeStateAction(gameData, (playerGameData) ->
                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
        }
    }

    @Override
    protected MoneyRabbitResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected MoneyRabbitGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected MoneyRabbitGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<MoneyRabbitPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return MoneyRabbitPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.MONEY_RABBIT;
    }
}
