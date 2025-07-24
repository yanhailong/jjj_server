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
     * 对局历史信息(50局的)
     */
    private final List<Integer> histories = new ArrayList<>();

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

    public void addHistory(int record) {
        int reduce = (histories.size() - LoongTigerWarConstant.Common.MAX_HISTORY) + 1;
        if (reduce > 0) {
            histories.clear();
        }
        histories.add(record);
    }

    public LoongTigerWarGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<Integer> getHistories() {
        return histories;
    }


}
