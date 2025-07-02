package com.jjg.game.room.data.room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对局历史
 *
 * @author 2CL
 */
public class GamePlayFlowPojo {

    /**
     * 记录房间玩家的每次操作
     */
    public static class RoomPlayerOperate {
        // 玩家ID
        private long playerId;
        // 输赢分数
        private long score;
        // 操作时间
        private long time;
    }

    /**
     * 记录每一轮数据，如果有多轮的对局
     */
    public static class RoundHistory {
        /**
         * 房间小局操作历史
         */
        private Map<Long, List<RoomPlayerOperate>> romePlayerOperateMap = new HashMap<>();

        public Map<Long, List<RoomPlayerOperate>> getRomePlayerOperateMap() {
            return romePlayerOperateMap;
        }

        public void setRomePlayerOperateMap(Map<Long, List<RoomPlayerOperate>> romePlayerOperateMap) {
            this.romePlayerOperateMap = romePlayerOperateMap;
        }

        // 单局对战结算信息，etc...
    }

    /**
     * 整局对战历史信息
     */
    public static class AllRoundHistory {
        // 每个小的单局历史记录
        private List<RoundHistory> roundHistory = new ArrayList<>();

        public List<RoundHistory> getRoundHistory() {
            return roundHistory;
        }

        public void setRoundHistory(List<RoundHistory> roundHistory) {
            this.roundHistory = roundHistory;
        }
    }
}
