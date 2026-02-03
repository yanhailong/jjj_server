package com.jjg.game.slots.game.zeusVsHades.manager;

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
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.game.thor.data.ThorPlayerGameData;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;
import com.jjg.game.slots.game.zeusVsHades.dao.ZeusVsHadesGameDataDao;
import com.jjg.game.slots.game.zeusVsHades.dao.ZeusVsHadesResultLibDao;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesGameRunInfo;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesPlayerGameData;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesPlayerGameDataDTO;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesResultLib;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jjg.game.common.proto.Pair.newPair;

public class AbstractZeusVsHadesGameManager extends AbstractSlotsGameManager<ZeusVsHadesPlayerGameData, ZeusVsHadesResultLib, ZeusVsHadesGameRunInfo> {
    @Autowired
    private ZeusVsHadesResultLibDao libDao;
    @Autowired
    private ZeusVsHadesGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsLogger logger;
    @Autowired
    private ZeusVsHadesGameDataDao gameDataDao;

    public AbstractZeusVsHadesGameManager() {
        super(ZeusVsHadesPlayerGameData.class, ZeusVsHadesResultLib.class, ZeusVsHadesGameRunInfo.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("启动宙斯vs哈里斯游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }

    @Override
    public ZeusVsHadesGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        ZeusVsHadesPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ZeusVsHadesGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        ZeusVsHadesGameRunInfo gameRunInfo = new ZeusVsHadesGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }


    /**
     * 开始游戏
     *
     * @param playerGameData
     * @param auto
     * @return
     */
    @Override
    public ZeusVsHadesGameRunInfo startGame(PlayerController playerController, ZeusVsHadesPlayerGameData playerGameData, long betValue, boolean auto) {
        ZeusVsHadesGameRunInfo gameRunInfo = new ZeusVsHadesGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(player.getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == ZeusVsHadesConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == ZeusVsHadesConstant.Status.CHOOSE_ONE) {  //二选一
                gameRunInfo.setCode(Code.FORBID);
                log.debug("当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            } else if (status == ZeusVsHadesConstant.Status.ZEUS) {
                gameRunInfo = free(gameRunInfo, playerGameData, ZeusVsHadesConstant.SpecialMode.ZEUS);
            } else if (status == ZeusVsHadesConstant.Status.HADES) {
                gameRunInfo = free(gameRunInfo, playerGameData, ZeusVsHadesConstant.SpecialMode.HADES);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", player.getId(), player.getGameType());
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(player, gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
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
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    @Override
    public ZeusVsHadesGameRunInfo normal(ZeusVsHadesGameRunInfo gameRunInfo, ZeusVsHadesPlayerGameData playerGameData, long betValue, ZeusVsHadesResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(ZeusVsHadesConstant.SpecialMode.CHOOSE) && generateManager.checkFreeModel(resultLib)) {  //是否会触发免费
            playerGameData.setStatus(ZeusVsHadesConstant.Status.CHOOSE_ONE);
            log.debug("触发二选一  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        //特殊 客户端开发要求推下次游戏状态 -》 所以注释
        gameRunInfo.setStatus(playerGameData.getStatus());

        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    public ZeusVsHadesGameRunInfo free(ZeusVsHadesGameRunInfo gameRunInfo, ZeusVsHadesPlayerGameData playerGameData, int specialModeFreeLibType) {
        CommonResult<ZeusVsHadesResultLib> libResult = freeGetLib(playerGameData, specialModeFreeLibType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount;
        if (playerGameData.isCount()) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        } else {
            afterCount = playerGameData.getRemainFreeCount().get();
        }
        log.debug("添加次数 ,afterCount = {}", afterCount);
        ZeusVsHadesResultLib freeGame = libResult.data;
//        if (freeGame.getAddFreeCount() > 0) {
//            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
//            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
//        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(ZeusVsHadesConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(ZeusVsHadesConstant.Status.ZEUS);

        return gameRunInfo;
    }

    /**
     * 免费模式二选一
     *
     * @param playerController
     * @return
     */
    public ZeusVsHadesGameRunInfo freeChooseOne(PlayerController playerController, int chooseType) {
        //获取玩家游戏数据
        ZeusVsHadesPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ZeusVsHadesGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        if (playerGameData.getStatus() != ThorConstant.Status.CHOOSE_ONE) {
            log.debug("玩家当前不处于二选一状态，二选一失败 playerId = {},gameType = {},roomCfgId = {},status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), playerGameData.getStatus());
            return new ZeusVsHadesGameRunInfo(Code.FORBID, playerController.playerId());
        }

        if (chooseType == 0) {
            playerGameData.setStatus(ZeusVsHadesConstant.Status.ZEUS);
        } else {
            playerGameData.setStatus(ZeusVsHadesConstant.Status.HADES);
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return new ZeusVsHadesGameRunInfo(Code.SUCCESS, playerController.playerId());
    }


    @Override
    public int getGameType() {
        return CoreConst.GameType.ZEUS_VS_HADES;
    }

    @Override
    protected void offlineSaveGameDataDto(ZeusVsHadesPlayerGameData gameData) {
        try {
            ZeusVsHadesPlayerGameDataDTO dto = gameData.converToDto(ZeusVsHadesPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected ZeusVsHadesResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected ZeusVsHadesGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected ZeusVsHadesGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭宙斯vs哈里斯游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void onAutoExitAction(ZeusVsHadesPlayerGameData playerGameData, int eventId) {
        //检查当前是否处于特殊模式
        if (playerGameData.getStatus() == BasketballSuperstarConstant.Status.FREE) {
            int forCount = playerGameData.getRemainFreeCount().get();
            while (forCount > 0) {
                autoStartGame(playerGameData, playerGameData.getAllBetScore());
                forCount = playerGameData.getRemainFreeCount().get();
            }
        }
    }

    @Override
    protected Class<ZeusVsHadesGameDataDao> getSlotsPlayerGameDataDTOCla() {
        return ZeusVsHadesGameDataDao.class;
    }

    /**
     * 自动玩游戏
     *
     * @param betValue
     * @return
     */
    public ZeusVsHadesGameRunInfo autoStartGame(ZeusVsHadesPlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动玩游戏 playerId = {}", playerGameData.playerId());
        return startGame(new PlayerController(null, null), playerGameData, betValue, true);
    }

    @Override
    protected CommonResult<ZeusVsHadesResultLib> freeGetLib(ZeusVsHadesPlayerGameData playerGameData, int specialModeFreeLibType, int specialAuxiliary) {
        CommonResult<ZeusVsHadesResultLib> result = new CommonResult<>(Code.SUCCESS);
        log.debug("开始获取免费结果库 playerId = {}", playerGameData.playerId());

        ZeusVsHadesResultLib freeLib = (ZeusVsHadesResultLib) playerGameData.getFreeLib();
        if (freeLib == null) {
            //缓存中没有，就从数据库获取
            CommonResult<ZeusVsHadesResultLib> libResult = getLibFromDB(playerGameData, specialModeFreeLibType);
            if (!libResult.success()) {
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
        Pair<ZeusVsHadesResultLib, Boolean> libPair = selectByLib(freeLib, index);
        log.debug("获取免费游戏的下标 index = {},allLen = {}", index, specialAuxiliaryInfo.getFreeGames().size());
        if(libPair == null){
            playerGameData.setFreeLib(null);
            result.code = Code.NOT_FOUND;
            return result;
        }
        ZeusVsHadesResultLib freeGame = libPair.getFirst();

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
        playerGameData.setCount(libPair.getSecond());
        result.data = freeGame;
        return result;
    }


    /**
     * 普通正常流程
     *
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    protected ZeusVsHadesGameRunInfo normal(ZeusVsHadesGameRunInfo gameRunInfo, ZeusVsHadesPlayerGameData playerGameData, long betValue) {
        //普通转 如果有免费转，先抽免费转
        ZeusVsHadesResultLib freeLib = (ZeusVsHadesResultLib) playerGameData.getFreeLib();
        if (freeLib != null) {
            gameRunInfo = free(gameRunInfo, playerGameData, ZeusVsHadesConstant.SpecialMode.NORMAL);
            return gameRunInfo;
        }

        CommonResult<Pair<ZeusVsHadesResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, 1);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        ZeusVsHadesResultLib resultLib = libResult.data.getFirst();
        if (resultLib == null) {
            log.debug("获取的结果为空 playerId = {},gameType = {},betValue = {}", playerGameData.playerId(), this.gameType, betValue);
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        if (resultLib.getSpecialAuxiliaryInfoList() != null && !resultLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            SpecialAuxiliaryInfo first = resultLib.getSpecialAuxiliaryInfoList().getFirst();
            if (first.getFreeGames() != null && !first.getFreeGames().isEmpty()) {
                playerGameData.setFreeLib(resultLib);
            }
        }
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        normal(gameRunInfo, playerGameData, betValue, resultLib);
        return gameRunInfo;
    }

    /**
     * @param SpecialAuxiliaryInfo
     * @param index
     * @return
     */
    public static Pair<ZeusVsHadesResultLib, Boolean> selectByLib(ZeusVsHadesResultLib fatherLib, int index) {
        // 使用队列进行广度优先遍历
        LinkedList<Pair<ZeusVsHadesResultLib,Boolean>> queue = new LinkedList<>();
        List<Pair<ZeusVsHadesResultLib,Boolean>> zeusVsHadesResultLibs = libList(queue,fatherLib,true);
        return zeusVsHadesResultLibs.get( index);
    }



    /**
     * @param SpecialAuxiliaryInfo
     * @param index
     * @return
     */
    public static List<Pair<ZeusVsHadesResultLib,Boolean>> libList( LinkedList<Pair<ZeusVsHadesResultLib,Boolean>> queue,ZeusVsHadesResultLib fatherLib,boolean isCount) {
        if (fatherLib.getSpecialAuxiliaryInfoList() != null && !fatherLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            SpecialAuxiliaryInfo first = fatherLib.getSpecialAuxiliaryInfoList().getFirst();
            if (first.getFreeGames() != null && !first.getFreeGames().isEmpty()) {
                for (JSONObject jsonObject : first.getFreeGames()) {
                    ZeusVsHadesResultLib lib = JSON.parseObject(jsonObject.toJSONString(), ZeusVsHadesResultLib.class);
                    Pair<ZeusVsHadesResultLib, Boolean> zeusVsHadesResultLibBooleanPair = newPair(lib, isCount);
                    queue.offer(zeusVsHadesResultLibBooleanPair);
                    libList(queue,lib,false);
                }
            }
        }
        return queue;
    }
}
