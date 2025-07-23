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
     * 对局历史信息(50局的)
     */
    private final List<RedBlackWarHistory> histories = new ArrayList<>();
    /**
     * 各区域押注信息区域id->玩家id->押注金额
     */
    private final Map<Integer, Map<Long, Long>> betInfo = new ConcurrentHashMap<>();

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
            histories.clear();
        }
        histories.add(addHistory);
    }

    public RedBlackWarGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<RedBlackWarHistory> getHistories() {
        return histories;
    }

    public Map<Integer, Map<Long, Long>> getBetInfo() {
        return betInfo;
    }

}
