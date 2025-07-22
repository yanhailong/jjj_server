package com.jjg.game.table.redblackwar.room.data;

import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import com.jjg.game.table.redblackwar.message.bean.RedBlackWarHistory;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarSettleInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战类游戏的内存常驻数据 Value Object
 *
 * @author 2CL
 */
public class RedBlackWarGameDataVo extends TableGameDataVo {

    /**
     * 阶段结束时间
     */
    private long phaseEndTime;
    /**
     * 对局历史信息(50局的)
     */
    private final List<RedBlackWarHistory> histories = new ArrayList<>();
    /**
     * 各区域押注信息区域id->玩家id->押注金额
     */
    private final Map<Integer, Map<Long, Long>> betInfo = new ConcurrentHashMap<>();

    /**
     * 本局前6的玩家id
     */
    private final List<Long> redBlackWarPlayerInfos = new ArrayList<>();

    /**
     * 本局的结算信息
     */
    private NotifyRedBlackWarSettleInfo currentSettleInfo;


    public NotifyRedBlackWarSettleInfo getCurrentSettleInfo() {
        return currentSettleInfo;
    }

    public void setCurrentSettleInfo(NotifyRedBlackWarSettleInfo currentSettleInfo) {
        this.currentSettleInfo = currentSettleInfo;
    }

    public void addHistory(RedBlackWarHistory addHistory) {
        int reduce = (histories.size() - RedBlackWarConstant.Common.MAX_HISTORY) + 1;
        if (reduce > 0) {
            for (int i = 0; i < reduce; i++) {
                if (histories.isEmpty()) {
                    continue;
                }
                histories.remove(0);
            }
        }
        histories.add(addHistory);
    }

    public RedBlackWarGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public long getPhaseEndTime() {
        return phaseEndTime;
    }

    public void setPhaseEndTime(long phaseEndTime) {
        this.phaseEndTime = phaseEndTime;
    }

    public List<RedBlackWarHistory> getHistories() {
        return histories;
    }

    public Map<Integer, Map<Long, Long>> getBetInfo() {
        return betInfo;
    }

    public List<Long> getRedBlackWarPlayerInfos() {
        return redBlackWarPlayerInfos;
    }
}
