package com.jjg.game.slots.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.dollarexpress.DollarExpressMessageHandler;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressTestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Scanner;


/**
 * @author 11
 * @date 2025/6/10 16:37
 */
@Component
public class SlotsStartManager implements IRoomStartListener {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsSampleManager slotsSampleManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private DollarExpressTestManager testManager;



    @Override
    public int[] getGameTypes() {
        return SlotsConst.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void start() {
        log.info("正在启动slots游戏...");

        this.slotsSampleManager.init();
        this.slotsPoolDao.initPool();
        initGameManager();
//        testManager.init();
    }

    @Override
    public void shutdown() {
        log.info("正在关闭slots游戏...");
    }

    /**
     * 初始化游戏管理器
     */
    private void initGameManager(){
        Map<String, AbstractSlotsGameManager> gameManagerMap = CommonUtil.getContext().getBeansOfType(AbstractSlotsGameManager.class);
        for(Map.Entry<String, AbstractSlotsGameManager> en : gameManagerMap.entrySet()){
            en.getValue().init();
        }
    }


}
