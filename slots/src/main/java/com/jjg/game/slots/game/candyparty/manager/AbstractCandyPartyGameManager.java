package com.jjg.game.slots.game.candyparty.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.TestLibData;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;
import com.jjg.game.slots.game.candyparty.dao.CandyPartyResultLibDao;
import com.jjg.game.slots.game.candyparty.data.CandyPartyGameRunInfo;
import com.jjg.game.slots.game.candyparty.data.CandyPartyPlayerGameData;
import com.jjg.game.slots.game.candyparty.data.CandyPartyResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

import java.util.List;
import java.util.Map;


public abstract class AbstractCandyPartyGameManager extends AbstractSlotsGameManager<CandyPartyPlayerGameData, CandyPartyResultLib, CandyPartyGameRunInfo> {
    protected final CandyPartyGameGenerateManager gameGenerateManager;
    protected final CandyPartyResultLibDao candyPartyResultLibDao;

    public AbstractCandyPartyGameManager(CandyPartyGameGenerateManager gameGenerateManager, CandyPartyResultLibDao candyPartyResultLibDao) {
        super(CandyPartyPlayerGameData.class, CandyPartyResultLib.class, CandyPartyGameRunInfo.class);
        this.gameGenerateManager = gameGenerateManager;
        this.candyPartyResultLibDao = candyPartyResultLibDao;
    }


    @Override
    public void init() {
        log.info("启动糖果派对游戏管理器...");
        super.init();

    }


    @Override
    protected CommonResult<CandyPartyResultLib> getLibFromDB(CandyPartyPlayerGameData playerGameData, int libType) {
        CommonResult<CandyPartyResultLib> result = new CommonResult<>(Code.SUCCESS);
        //先去获取测试数据
        TestLibData testLibData = playerGameData.pollTestLibData();

        boolean gmLibType = false;
        CandyPartyResultLib resultLib = null;
        if (testLibData != null) {
            libType = testLibData.getLibType();
            if (libType > 0) {
                gmLibType = true;
                log.debug("获取到测试数据 playerId = {},libType = {}", playerGameData.getPlayerId(), libType);
            } else if (testLibData.getData() != null) {
                resultLib = (CandyPartyResultLib) testLibData.getData();
                log.debug("获取到测试数据 playerId = {},lib = {}", playerGameData.getPlayerId(), JSON.toJSONString(resultLib));
            }
        }

        if (resultLib == null) {
            //获取倍场配置
            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
            if (baseRoomCfg == null) {
                log.warn("获取倍场配置失败 playerId = {},gameType = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId());
                result.code = Code.NOT_FOUND;
                return result;
            }

            //获取结果库配置
            CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
            if (!libCfgResult.success()) {
                if (libCfgResult.code == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
                    sendRoomAmountNotEnough(playerGameData.getPlayerId());
                }
                result.code = libCfgResult.code;
                return result;
            }

            //如果gm中没有设置 libType，则需要根据配置获取 libType
            if (!gmLibType && CandyPartyConstant.SpecialMode.NORMAL_MAP.containsValue(libType)) {
                //获取 specialResultLib 中的type
                CommonResult<Integer> resultLibTypeResult = getResultLibType(playerGameData, libCfgResult.data.getModelId(), playerGameData.getRoomType());
                if (!resultLibTypeResult.success()) {
                    result.code = resultLibTypeResult.code;
                    return result;
                }
                libType = resultLibTypeResult.data;
                log.debug("获取到结果库类型 playerId = {},libType = {}", playerGameData.getPlayerId(), libType);
            }

            //如果获取结果库失败，会重试，所以用循环
            for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
                //根据倍数区间从结果库里面随机获取一条
                resultLib = getLib(libCfgResult.data, libType, playerGameData);
                if (resultLib != null) {
                    //检查该lib是否中奖jackpot
                    if (resultLib.getJackpotIds() != null && !resultLib.getJackpotIds().isEmpty()) {
                        List<Integer> rewardPoolIds = checkLibPool(resultLib, playerGameData);
                        if (rewardPoolIds.isEmpty()) {  //如果发现lib可以中奖，但是不够资格
                            resultLib = afterForbidPoolLib(libCfgResult.data, resultLib, playerGameData);
                        }
                    }
                }

                if (resultLib == null) {
                    log.warn("获取结果库失败 gameType = {},modelId = {},libType = {},retry = {}", this.gameType, libCfgResult.data.getModelId(), libType, i);
                    continue;
                }
//                sectionIndex = resultLibSectionResult.data;
                log.info("成功获取结果库  playerId = {}", playerGameData.getPlayerId());
                result.code = Code.SUCCESS;
                playerGameData.setLastModelId(libCfgResult.data.getModelId());
                break;
            }
        }

        //如果前面没有获取到lib，则获取一个无奖励的结果
        if (resultLib == null) {
            int normalLibType = CandyPartyConstant.SpecialMode.NORMAL_MAP.getOrDefault(playerGameData.getLayerNumber(), 0);
            resultLib = getResultLibDao().getLibBySectionIndex(normalLibType, 0, this.libClass);
            if (resultLib == null) {
                log.warn("前面没有获取到lib失败，获取不中奖的结果也失败 playerId = {}", playerGameData.getPlayerId());
                result.code = Code.FAIL;
                return result;
            }
        }
        result.data = resultLib;
        return result;
    }

    @Override
    protected CandyPartyGameRunInfo normal(CandyPartyGameRunInfo gameRunInfo, CandyPartyPlayerGameData playerGameData, long betValue) {
        Integer model = CandyPartyConstant.SpecialMode.NORMAL_MAP.get(playerGameData.getLayerNumber());
        if (model == null) {
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        //根据玩家当前层数获取数据
        CommonResult<Pair<CandyPartyResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, model);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        CandyPartyResultLib resultLib = libResult.data.getFirst();
        if (resultLib == null) {
            log.debug("获取的结果为空 playerId = {},gameType = {},betValue = {}", playerGameData.getPlayerId(), this.gameType, betValue);
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        normal(gameRunInfo, playerGameData, betValue, resultLib);
        return gameRunInfo;
    }


    /**
     * 获取 specialResultLib 中的type
     *
     * @param modelId
     * @return
     */
    @Override
    protected CommonResult<Integer> getResultLibType(CandyPartyPlayerGameData playerGameData, int modelId, RoomType roomType) {
        CommonResult<Integer> resultLibType = super.getResultLibType(playerGameData, modelId, roomType);
        if (!resultLibType.success()) {
            return resultLibType;
        }
        //根据玩家的层数获取id
        if (CandyPartyConstant.SpecialMode.JACKPOT_MAP.containsValue(resultLibType.data)) {
            Integer realType = CandyPartyConstant.SpecialMode.JACKPOT_MAP.get(playerGameData.getLayerNumber());
            if (realType != null) {
                resultLibType.data = realType;
            }
        }
        //根据玩家的层数获取id
        if (CandyPartyConstant.SpecialMode.FREE_MAP.containsValue(resultLibType.data)) {
            Integer realType = CandyPartyConstant.SpecialMode.FREE_MAP.get(playerGameData.getLayerNumber());
            if (realType != null) {
                resultLibType.data = realType;
            }
        }
        if (CandyPartyConstant.SpecialMode.NORMAL_MAP.containsValue(resultLibType.data)) {
            Integer realType = CandyPartyConstant.SpecialMode.NORMAL_MAP.get(playerGameData.getLayerNumber());
            if (realType != null) {
                resultLibType.data = realType;
            }
        }
        return resultLibType;
    }

    /**
     * 开始游戏
     *
     */
    @Override
    public CandyPartyGameRunInfo startGame(PlayerController playerController, CandyPartyPlayerGameData playerGameData, long betValue, boolean auto) {
        CandyPartyGameRunInfo gameRunInfo = new CandyPartyGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));
            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == CandyPartyConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == CandyPartyConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }
            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //设置当前层数
            gameRunInfo.setLayerNumber(playerGameData.getLayerNumber());
            //检查收集的元素信息
            checkElementCollection(gameRunInfo, playerGameData);
            //设置剩余元素数量
            gameRunInfo.setRemainIconNum(playerGameData.getCollectedIconNum());
            gameRunInfo.setNextLayerNumber(playerGameData.getLayerNumber());
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
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    public void checkElementCollection(CandyPartyGameRunInfo gameRunInfo, CandyPartyPlayerGameData playerGameData) {

        //收集图标
        Map<Integer, Pair<Integer, Integer>> passingCriteriaMap = gameGenerateManager.getPassingCriteriaMap();
        if (CollectionUtil.isEmpty(passingCriteriaMap)) {
            return;
        }
        Pair<Integer, Integer> pair = passingCriteriaMap.get(playerGameData.getLayerNumber());
        if (pair == null) {
            return;
        }
        CandyPartyResultLib resultLib = (CandyPartyResultLib) gameRunInfo.getResultLib();
        gameRunInfo.setCollectedIconNum(resultLib.getElementCollectionNum());
        int collectedIconNum = playerGameData.getCollectedIconNum();
        int total = resultLib.getElementCollectionNum() + collectedIconNum;
        int remain = total - pair.getSecond();
        //免费模式不切换
        if (remain >= 0 && playerGameData.getStatus() != CandyPartyConstant.Status.FREE) {
            //第三层到第一层不继承收集数量
            if (playerGameData.getLayerNumber() == CandyPartyConstant.Common.MAX_LAYER) {
                remain = 0;
            }
            //设置下一层,以及收集的数量
            int layerNumber = (playerGameData.getLayerNumber() % CandyPartyConstant.Common.MAX_LAYER) + 1;
            playerGameData.setLayerNumber(layerNumber);
            playerGameData.setCollectedIconNum(remain);
            return;
        }
        playerGameData.setCollectedIconNum(total);
    }

    @Override
    protected CandyPartyGameRunInfo normal(CandyPartyGameRunInfo gameRunInfo, CandyPartyPlayerGameData playerGameData, long betValue, CandyPartyResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        int freeModel = CandyPartyConstant.SpecialMode.FREE_MAP.getOrDefault(playerGameData.getLayerNumber(), 0);
        if (resultLib.getLibTypeSet().contains(freeModel)) {  //是否会触发免费
            playerGameData.setStatus(CandyPartyConstant.Status.FREE);
            playerGameData.setFreeLib(resultLib);
            if (CollectionUtil.isNotEmpty(resultLib.getSpecialAuxiliaryInfoList())) {
                for (SpecialAuxiliaryInfo specialAuxiliaryInfo : resultLib.getSpecialAuxiliaryInfoList()) {
                    if (CollectionUtil.isEmpty(specialAuxiliaryInfo.getFreeGames())) {
                        continue;
                    }
                    playerGameData.getRemainFreeCount().set(specialAuxiliaryInfo.getFreeGames().size());
                }
            }
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
            gameRunInfo.setFreeGameMultiple(resultLib.getFreeGameMultiple());
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus(),
                    playerGameData.getRemainFreeCount().get(), resultLib.getTimes());
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }
        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        log.debug("id = {}", resultLib.getId());
        gameRunInfo.setIconArr(resultLib.getIconArr());
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
    protected void free(CandyPartyGameRunInfo gameRunInfo, CandyPartyPlayerGameData playerGameData) {
        Integer freeMode = CandyPartyConstant.SpecialMode.FREE_MAP.get(playerGameData.getLayerNumber());
        if (freeMode == null) {
            gameRunInfo.setCode(Code.FAIL);
            return;
        }

        CommonResult<CandyPartyResultLib> libResult = freeGetLib(playerGameData, freeMode);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        CandyPartyResultLib freeGame = libResult.data;
        gameRunInfo.setStatus(playerGameData.getStatus());
        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        CandyPartyResultLib freeLib = playerGameData.getFreeLib();
        gameRunInfo.setFreeGameMultiple(freeLib.getFreeGameMultiple());
        if (afterCount == 0) {
            playerGameData.setStatus(CandyPartyConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.CANDY_PARTY;
    }

    @Override
    protected CandyPartyResultLibDao getResultLibDao() {
        return this.candyPartyResultLibDao;
    }

    @Override
    protected CandyPartyGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }


    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭Captain Jack游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }


}
