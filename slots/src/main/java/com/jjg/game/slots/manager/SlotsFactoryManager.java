package com.jjg.game.slots.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.GameStatus;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/8/22 9:53
 */
@Component
public class SlotsFactoryManager {

    private static final Logger log = LoggerFactory.getLogger(SlotsFactoryManager.class);
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsRoomManager slotsRoomManager;
    @Autowired
    private CoopRoomManager coopRoomManager;
    @Autowired
    private GameStatusService gameStatusService;

    //所有的游戏管理器
    private Map<Integer, AbstractSlotsGameManager> slotsGameManagerMap = new HashMap<>();
    //所有的游戏管理器
    private Map<Integer, AbstractSlotsGameManager> slotsRoomGameManagerMap = new HashMap<>();

    /**
     * 工厂初始化
     */
    public void init(ApplicationContext context) {
        //初始化池子
        this.slotsPoolDao.initPool();
        //初始化游戏管理器
        initGameManager(context);
        this.slotsRoomManager.init();
        this.coopRoomManager.init();
        //刷新游戏状态
        refreshGameStatus();
    }

    public void onEnterGame(long playerId, int roomCfgId, long roomId) {
        exitOldPlayerGameDataOnEnter(slotsGameManagerMap, playerId, roomCfgId, roomId);
        exitOldPlayerGameDataOnEnter(slotsRoomGameManagerMap, playerId, roomCfgId, roomId);
    }

    private void exitOldPlayerGameDataOnEnter(Map<Integer, AbstractSlotsGameManager> gameManagerMap, long playerId, int roomCfgId, long roomId) {
        for (AbstractSlotsGameManager<?, ?, ?> gameManager : gameManagerMap.values()) {
            gameManager.exitOldPlayerGameDataOnEnter(playerId, roomCfgId, roomId);
        }
    }

    /**
     * 初始化游戏管理器
     */
    private void initGameManager(ApplicationContext context) {
        Map<String, AbstractSlotsGameManager> gameManages = context.getBeansOfType(AbstractSlotsGameManager.class);
        gameManages.forEach((k, v) -> {
            try {
                v.init();
                int gameType = v.getGameType();
                if (v.getRoomType() == null) {
                    this.slotsGameManagerMap.put(gameType, v);
                } else if (v.getRoomType() == RoomType.SLOTS_TEAM_UP_ROOM) {
                    this.slotsRoomGameManagerMap.put(gameType, v);
                } else {
                    throw new RuntimeException("roomType not support  " + v.getRoomType());
                }
            } catch (Exception e) {
                log.error("gameType={}", v.getGameType(), e);
            }

        });
    }

    /**
     * 关闭游戏管理器
     */
    private void closeGameManager() {
        Set<AbstractSlotsGameManager> allManagers = new HashSet<>();
        allManagers.addAll(this.slotsGameManagerMap.values());
        allManagers.addAll(this.slotsRoomGameManagerMap.values());
        allManagers.forEach(AbstractSlotsGameManager::shutdown);
    }

    public AbstractSlotsGameManager getGameManager(int gameType, int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg != null) {
            if (warehouseCfg.getRoomType() < GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
                return this.slotsGameManagerMap.get(gameType);
            }

            if (warehouseCfg.getRoomType() < GameConstant.RoomTypeCons.SVIP_ROOM_TYPE_START) {
                return this.slotsRoomGameManagerMap.get(gameType);
            }
        }
        return this.slotsGameManagerMap.get(gameType);
    }

    public AbstractSlotsGameManager getGameManager(int gameType) {
        return this.slotsGameManagerMap.get(gameType);
    }


    /**
     * 刷新游戏状态
     */
    public void refreshGameStatus() {
        List<GameStatus> allGameStatus = gameStatusService.getAllGameStatus();
        if (allGameStatus == null || allGameStatus.isEmpty()) {
            Map<String, AbstractSlotsGameManager> gameManages = CommonUtil.getContext().getBeansOfType(AbstractSlotsGameManager.class);
            gameManages.forEach((k, v) -> {
                v.getOpen().compareAndSet(true, false);
            });
            return;
        }

        Map<Integer, GameStatus> statusMap = allGameStatus.stream()
                .collect(Collectors.toMap(GameStatus::gameId, gameStatus -> gameStatus));

        Map<String, AbstractSlotsGameManager> gameManages = CommonUtil.getContext().getBeansOfType(AbstractSlotsGameManager.class);
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

    /**
     * 关闭工厂
     */
    public void shutdown() {
        //摘流后必须先确认协作房间已全部自然结束；有房间时拒绝关闭且不修改任何游戏管理器状态
        this.coopRoomManager.shutdown();
        closeGameManager();
        this.slotsRoomManager.shutDown();
    }
}
