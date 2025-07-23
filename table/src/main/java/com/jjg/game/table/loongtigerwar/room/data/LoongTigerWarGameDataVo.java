package com.jjg.game.table.loongtigerwar.room.data;

import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.loongtigerwar.constant.LoongTigerWarConstant;
import com.jjg.game.table.loongtigerwar.message.resp.NotifyLoongTigerWarSettleInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战类游戏的内存常驻数据 Value Object
 *
 * @author 2CL
 */
public class LoongTigerWarGameDataVo extends TableGameDataVo {

    /**
     * 阶段结束时间
     */
    private long phaseEndTime;
    /**
     * 对局历史信息(50局的)
     */
    private final List<Integer> histories = new ArrayList<>();
    /**
     * 各区域押注信息区域id->玩家id->押注金额
     */
    private final Map<Integer, Map<Long, Long>> betInfo = new ConcurrentHashMap<>();

    /**
     * 前6玩家的id
     */
    private final List<Long> fixPlayers = new ArrayList<>();
    /**
     * 本局的结算信息
     */
    private NotifyLoongTigerWarSettleInfo currentSettleInfo;


    public NotifyLoongTigerWarSettleInfo getCurrentSettleInfo() {
        return currentSettleInfo;
    }

    public void setCurrentSettleInfo(NotifyLoongTigerWarSettleInfo currentSettleInfo) {
        this.currentSettleInfo = currentSettleInfo;
    }

    public List<Long> getFixPlayers() {
        return fixPlayers;
    }

    public void addHistory(int record) {
        int reduce = (histories.size() - LoongTigerWarConstant.Common.MAX_HISTORY) + 1;
        if (reduce > 0) {
            for (int i = 0; i < reduce; i++) {
                if (histories.isEmpty()) {
                    continue;
                }
                histories.remove(0);
            }
        }
        histories.add(record);
    }

    public LoongTigerWarGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public long getPhaseEndTime() {
        return phaseEndTime;
    }

    public void setPhaseEndTime(long phaseEndTime) {
        this.phaseEndTime = phaseEndTime;
    }

    public List<Integer> getHistories() {
        return histories;
    }

    public Map<Integer, Map<Long, Long>> getBetInfo() {
        return betInfo;
    }

}
