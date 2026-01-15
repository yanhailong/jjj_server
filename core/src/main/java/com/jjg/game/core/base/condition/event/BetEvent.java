package com.jjg.game.core.base.condition.event;

import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author lm
 * @date 2026/1/14 10:36
 */
public class BetEvent {
    /**
     * 游戏id warehouse.xlsx id
     */
    private int gameId;
    /**
     * 游戏房间类型 warehouse.xlsx roomType
     */
    private int roomType;
    /**
     * 游戏大类型 warehouse.xlsx gameType
     */
    private int gameType;
    /**
     * 押注总值
     */
    private long betAmount;

    private long winAmount;
    private List<Integer> betList;
    /**
     * 货币类型
     */
    private int itemId;

    public List<Integer> getBetList() {
        return betList;
    }

    public void setBetList(List<Integer> betList) {
        this.betList = betList;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long betAmount) {
        this.betAmount = betAmount;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public long getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(long winAmount) {
        this.winAmount = winAmount;
    }

    /**
     * 通过有效流水事件构建对象
     * @param event    事件
     * @return PlayerEffectiveParam
     */
    public static BetEvent getPlayerEffectiveParam(PlayerEventCategory.PlayerEffectiveFlowingEvent event) {
        int gameCfgId = event.getGameCfgId();
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(gameCfgId);
        if (warehouseCfg == null) {
            return null;
        }
        BetEvent param = new BetEvent();
        param.setGameId(gameCfgId);
        param.setGameType(warehouseCfg.getGameType());
        param.setRoomType(warehouseCfg.getRoomType());
        if (event.getEventChangeValue() instanceof Long value) {
            param.setBetAmount(value);
        }
        return param;
    }
}

