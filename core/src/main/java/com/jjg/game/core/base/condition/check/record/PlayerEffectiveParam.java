package com.jjg.game.core.base.condition.check.record;

import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class PlayerEffectiveParam extends BaseCheckParam {
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
     * 附加参数 根据不同需要按顺序放入，具体看使用类
     */
    private List<Long> paramList;

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

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public List<Long> getParamList() {
        return paramList;
    }

    public void setParamList(List<Long> paramList) {
        this.paramList = paramList;
    }

    /**
     * 通过有效流水事件构建对象
     *
     * @param function 功能名称
     * @param playerId 玩家id
     * @param event 事件
     * @return PlayerEffectiveParam
     */
    public static PlayerEffectiveParam getPlayerEffectiveParam(String function, long playerId, PlayerEventCategory.PlayerEffectiveFlowingEvent event) {
        int gameCfgId = event.getGameCfgId();
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(gameCfgId);
        if (warehouseCfg == null) {
            return null;
        }
        PlayerEffectiveParam param = new PlayerEffectiveParam();
        param.setPlayerId(playerId);
        param.setGameId(gameCfgId);
        param.setGameType(warehouseCfg.getGameType());
        param.setRoomType(warehouseCfg.getRoomType());
        if (event.getEventChangeValue() instanceof Long value) {
            param.setParamList(List.of(value));
        }
        if (StringUtils.isNotEmpty(function)) {
            param.setFunction(function);
        }
        return param;
    }
}
