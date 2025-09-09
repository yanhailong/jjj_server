package com.jjg.game.slots.game.mahjiongwin.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinGameDataDao;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinResultLibDao;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 麻将胡了游戏逻辑处理器
 *
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class MahjiongWinGameManager extends AbstractSlotsGameManager<MahjiongWinPlayerGameData,MahjiongWinResultLib> {
    @Autowired
    private MahjiongWinResultLibDao libDao;
    @Autowired
    private MajiongWinGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsLogger logger;
    @Autowired
    private MahjiongWinGameDataDao gameDataDao;

    public MahjiongWinGameManager() {
        super(MahjiongWinPlayerGameData.class,MahjiongWinResultLib.class);
    }

    @Override
    public void init() {
        log.info("启动麻将胡了游戏管理器...");
        super.init();
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
            }
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
            log.debug("触发免费模式  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        gameRunInfo.setBet(betValue);
        gameRunInfo.setIconArr(resultLib.getIconArr());

        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    private MahjiongWinGameRunInfo free(MahjiongWinGameRunInfo gameRunInfo, MahjiongWinPlayerGameData playerGameData){
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
    protected MajiongWinGenerateManager getGenerateManager() {
        return this.generateManager;
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
