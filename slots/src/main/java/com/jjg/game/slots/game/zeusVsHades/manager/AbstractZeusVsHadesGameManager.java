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
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.thor.ThorConstant;
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

import java.util.concurrent.atomic.AtomicInteger;

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
            } else if (status == ZeusVsHadesConstant.Status.HADES) {
                gameRunInfo = free(gameRunInfo, playerGameData);
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
        if (resultLib.getLibTypeSet().contains(ZeusVsHadesConstant.SpecialMode.ZEUS)) {  //是否会触发免费
            playerGameData.setStatus(ZeusVsHadesConstant.Status.ZEUS);
            int againFreeCount = 0;
            int allCount = 0;
            for (SpecialAuxiliaryInfo info : resultLib.getSpecialAuxiliaryInfoList()) {
                for (JSONObject json : info.getFreeGames()) {
                    Integer addFreeCount = json.getInteger("addFreeCount");
                    if (addFreeCount != null && addFreeCount > 0) {
                        againFreeCount += addFreeCount;
                    }
                }
                allCount += info.getFreeGames().size();
            }
            //设置添加的免费次数
            int addCount = allCount - againFreeCount;
            playerGameData.setRemainFreeCount(new AtomicInteger(addCount));

            long times = generateManager.calLineTimes(resultLib.getAwardLineInfoList());

            playerGameData.setFreeLib(resultLib);

            gameRunInfo.addBigPoolTimes(times);
            //特殊 客户端开发要求推下次游戏状态 -》 赋值状态 免费转
//            gameRunInfo.setStatus(ZeusVsHadesConstant.Status.ZEUS);
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
//            //特殊 客户端开发要求推下次游戏状态 -》 赋值状态 免费转
//            gameRunInfo.setStatus(ZeusVsHadesConstant.Status.NORMAL);
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        //特殊 客户端开发要求推下次游戏状态 -》 所以注释
        gameRunInfo.setStatus(ZeusVsHadesConstant.Status.NORMAL);

        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    public ZeusVsHadesGameRunInfo free(ZeusVsHadesGameRunInfo gameRunInfo, ZeusVsHadesPlayerGameData playerGameData) {
        CommonResult<ZeusVsHadesResultLib> libResult = freeGetLib(playerGameData, ZeusVsHadesConstant.SpecialMode.ZEUS);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        ZeusVsHadesResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

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
}
