package com.jjg.game.slots.game.mahjiongwin.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinGameDataDao;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinResultLibDao;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 麻将胡了游戏逻辑处理器
 *
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class MahjiongWinGameManager extends AbstractSlotsGameManager<MahjiongWinPlayerGameData, MahjiongWinResultLib> {
    @Autowired
    private MahjiongWinResultLibDao libDao;
    @Autowired
    private MahjiongWinGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsLogger logger;
    @Autowired
    private MahjiongWinGameDataDao gameDataDao;

    public MahjiongWinGameManager() {
        super(MahjiongWinPlayerGameData.class, MahjiongWinResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("启动麻将胡了游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }

    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public MahjiongWinGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        MahjiongWinPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new MahjiongWinGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param auto
     * @return
     */
    public MahjiongWinGameRunInfo startGame(PlayerController playerController, MahjiongWinPlayerGameData playerGameData, long betValue, boolean auto) {
        MahjiongWinGameRunInfo gameRunInfo = new MahjiongWinGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == MahjiongWinConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == MahjiongWinConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            gameRunInfo.setAfterGold(player.getGold());
            if (playerController != null) {
                playerController.setPlayer(player);
            }

            //添加大奖展示id
            int times = (int) (gameRunInfo.getAllWinGold() / betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
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
    private MahjiongWinGameRunInfo normal(MahjiongWinGameRunInfo gameRunInfo, MahjiongWinPlayerGameData playerGameData, long betValue) {
        CommonResult<MahjiongWinResultLib> libResult = normalGetLib(playerGameData, betValue);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        MahjiongWinResultLib resultLib = libResult.data;

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(MahjiongWinConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(MahjiongWinConstant.Status.FREE);
            int[] freeCount = new int[1];
            //设置剩余次数
            resultLib.getSpecialAuxiliaryInfoList().forEach(info -> info.getFreeGames().forEach(json -> {
                Integer addFreeCount = json.getInteger("addFreeCount");
                if (addFreeCount == null || addFreeCount < 1) {
                    freeCount[0] = freeCount[0] + 1;
                }
            }));
            playerGameData.setRemainFreeCount(new AtomicInteger(freeCount[0]));

            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(),freeCount[0]);
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private MahjiongWinGameRunInfo free(MahjiongWinGameRunInfo gameRunInfo, MahjiongWinPlayerGameData playerGameData) {
        CommonResult<MahjiongWinResultLib> libResult = freeGetLib(playerGameData);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        MahjiongWinResultLib freeGame = libResult.data;
        if(freeGame.getAddFreeCount() > 0){
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(),afterCount);
        }

        if (afterCount < 1) {
            playerGameData.setStatus(MahjiongWinConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());

        return gameRunInfo;
    }


    @Override
    public int getGameType() {
        return CoreConst.GameType.MAHJIONG_WIN;
    }

    @Override
    protected void offlineSaveGameDataDto(MahjiongWinPlayerGameData gameData) {

    }

    @Override
    protected MahjiongWinResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected MahjiongWinGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected MahjiongWinGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭麻将胡了游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }


}
