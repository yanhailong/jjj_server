package com.jjg.game.ploy.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.GameStatus;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.ploy.controller.AbstractPloyController;
import com.jjg.game.ploy.dao.PloyPoolDao;
import com.jjg.game.ploy.handler.PloyMessageHandler;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2026/5/9
 */
@Component
public class PloyFactoryManager {
    private final Logger log = LoggerFactory.getLogger(PloyFactoryManager.class);

    @Autowired
    private PloyPoolDao poolDao;
    @Autowired
    private GameStatusService gameStatusService;

    //所有的游戏控制器
    private Map<Integer, AbstractPloyController> slotsGameManagerMap = new HashMap<>();

    /**
     * 工厂初始化
     */
    public void init(ApplicationContext context) {
        //初始化池子
        this.poolDao.initPool();
        //初始化游戏管理器
        initGameManager(context);
        //刷新游戏状态
        refreshGameStatus();
    }

    /**
     * 初始化游戏管理器
     */
    private void initGameManager(ApplicationContext context) {
        Map<String, AbstractPloyController> gameManages = context.getBeansOfType(AbstractPloyController.class);
        gameManages.forEach((k, v) -> {
            v.init();
            this.slotsGameManagerMap.put(v.getGameType(), v);
        });
    }

    /**
     * 刷新游戏状态
     */
    public void refreshGameStatus() {
        List<GameStatus> allGameStatus = gameStatusService.getAllGameStatus();
        if (allGameStatus == null || allGameStatus.isEmpty()) {
            Map<String, AbstractPloyController> gameManages = CommonUtil.getContext().getBeansOfType(AbstractPloyController.class);
            gameManages.forEach((k, v) -> {
                v.getOpen().compareAndSet(true, false);
            });
            return;
        }

        Map<Integer, GameStatus> statusMap = allGameStatus.stream()
                .collect(Collectors.toMap(GameStatus::gameId, gameStatus -> gameStatus));

        Map<String, AbstractPloyController> gameManages = CommonUtil.getContext().getBeansOfType(AbstractPloyController.class);
        gameManages.forEach((k, v) -> {
            int gameType = v.getGameType();
            GameStatus gameStatus = statusMap.get(gameType);
            if (gameStatus != null && gameStatus.status() == 1 && gameStatus.open() == 1) {
                v.getOpen().compareAndSet(false, true);
            } else {
                v.getOpen().compareAndSet(true, false);
            }
        });
    }

    public AbstractPloyController getGameController(int gameType, int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg != null) {
            if (warehouseCfg.getRoomType() < GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
                return this.slotsGameManagerMap.get(gameType);
            }
        }
        return this.slotsGameManagerMap.get(gameType);
    }

    public void shutdown() {
        for (Map.Entry<Integer, AbstractPloyController> en : this.slotsGameManagerMap.entrySet()) {
            try {
                en.getValue().shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}
