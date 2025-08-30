package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.data.GirdUpdatePropConfig;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressGameDataDao;
import com.jjg.game.slots.game.dollarexpress.data.*;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 美元快递游戏逻辑处理器
 *
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressGameManager extends AbstractSlotsGameManager<DollarExpressPlayerGameData> {

    @Autowired
    private DollarExpressResultLibDao libDao;
    @Autowired
    private DollarExpressGenerateManager generateManager;
    @Autowired
    private DollarExpressGameDataDao gameDataDao;

    //玩家自动二选一定时任务
    private Map<Long, TimerEvent<String>> autoChooseFreeModeEventMap = new HashMap<>();
    //玩家投资游戏定时任务
    private Map<Long, TimerEvent<String>> autoInversEventMap = new HashMap<>();

    private Map<Integer, GirdUpdatePropConfig> girdUpdateConfigMap;
    //在替换格子时限制while最大循环次数
    private final int updateGirdWhildMaxCount = 30;

    //倍率放大倍数
    private int timesScale = 100;
    private BigDecimal timesScaleBigDecimal = BigDecimal.valueOf(timesScale);


    public DollarExpressGameManager() {
        super(DollarExpressPlayerGameData.class);
    }

    @Override
    public void init() {
        log.info("启动美元快递游戏管理器...");
        super.init();
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param betValue
     * @return
     */
    public DollarExpressGameRunInfo playerStartGame(PlayerController playerController, long betValue) {
        return null;
    }

    /**
     * 玩家二选一
     *
     * @param playerController
     * @param chooseStatus
     * @return
     */
    public DollarExpressGameRunInfo playerChooseFreeGameType(PlayerController playerController, int chooseStatus) {
        return null;
    }

    /**
     * 玩家投资游戏选择地区
     *
     * @param playerController
     * @param areaId
     * @return
     */
    public DollarExpressGameRunInfo playerInvest(PlayerController playerController, int areaId) {
        return null;
    }

    /**
     * 获取奖池
     *
     * @param playerController
     */
    public DollarExpressGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        return null;
    }

    @Override
    protected void offlineSaveGameDataDto(DollarExpressPlayerGameData gameData) {

    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.DOLLAR_EXPRESS;
    }

    /**
     * 添加测试icons
     *
     * @param playerController
     * @param testLibData
     */
    public void addTestIconData(PlayerController playerController, TestLibData testLibData, boolean icons) {

    }

    /**
     * 测试使用，选择所有地区，只剩一个可选
     *
     * @param playerController
     */
    public void selectAllArea(PlayerController playerController) {
        DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return;
        }
        for (int i = 1; i < 8; i++) {
            playerGameData.addSelectedArea(i);
        }
    }

}
