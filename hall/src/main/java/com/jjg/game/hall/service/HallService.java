package com.jjg.game.hall.service;

import com.jjg.game.core.data.GameStatus;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.WarehouseCfg;
import com.jjg.game.hall.sample.bean.GameListCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/18 14:56
 */
@Component
public class HallService implements ConfigExcelChangeListener {
    @Autowired
    private GameStatusService gameStatusService;

    private Map<Integer, List<WareHouseConfigInfo>> wareHouseConfigMap = new HashMap<>();
    //游戏类型->游戏状态
    private Map<Integer, GameStatus> gameStatusesMap;

    public Map<Integer, GameStatus> getGameStatusesMap() {
        return gameStatusesMap;
    }

    public void loadGameStatuses(List<GameStatus> gameStatuses) {
        if (Objects.nonNull(gameStatuses)) {
            gameStatusesMap = gameStatuses.stream().collect(Collectors.toMap(GameStatus::gameId, gs -> gs));
        }
    }

    public void refreshGameStatuses() {
        loadGameStatuses(gameStatusService.getAllGameStatus());
    }

    public void init() {
        initWareHouseConfigData();
        loadGameStatuses(gameStatusService.getAllGameStatus());
    }

    public List<WareHouseConfigInfo> getWareHouseConfigByGameType(int gameType) {
        return wareHouseConfigMap.get(gameType);
    }

    public boolean canJoinGame(int gameType) {
        GameStatus gameStatus = gameStatusesMap.get(gameType);
        if (Objects.nonNull(gameStatus)) {
            return gameStatus.open() == 2 && gameStatus.status() == 2;
        }
        GameListCfg gameListCfg = GameDataManager.getGameListCfg(gameType);
        if (Objects.nonNull(gameListCfg)) {
            return gameListCfg.getStatus() == HallCode.GAME_STATUS_OPEN;
        }
        return false;
    }

    @Override
    public void change(String className) {
        if (className.equalsIgnoreCase(WarehouseCfg.class.getSimpleName())) {
            initWareHouseConfigData();
        }
    }

    private void initWareHouseConfigData() {
        Map<Integer, List<WareHouseConfigInfo>> tempwareHouseConfigMap = new HashMap<>();

        for (WarehouseCfg c : GameDataManager.getWarehouseCfgList()) {
            List<WareHouseConfigInfo> tempList = tempwareHouseConfigMap.computeIfAbsent(c.getGameID(),
                k -> new ArrayList<>());
            WareHouseConfigInfo info = new WareHouseConfigInfo();
            info.wareId = c.getId() - (c.getGameID() * 10);
            info.pool = 99999L;
            info.limitGoldMin = c.getEnterLimit();
            info.limitVipMin = c.getVipLvLimit();
            info.betShow = c.getBetShow();
            tempList.add(info);
        }
        this.wareHouseConfigMap = tempwareHouseConfigMap;
    }
}
