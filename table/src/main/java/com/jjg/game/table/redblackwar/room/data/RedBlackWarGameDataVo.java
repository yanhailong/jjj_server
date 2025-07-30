package com.jjg.game.table.redblackwar.room.data;

import com.jjg.game.core.data.Card;
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
     * gm修改的
     */
    private List<Card> black;
    /**
     * gm修改的
     */
    private List<Card> red;

    /**
     * 对局历史信息(50局的)
     */
    private final List<RedBlackWarHistory> histories = new ArrayList<>();
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

    public List<Card> getBlack() {
        return black;
    }

    public void setBlack(List<Card> black) {
        this.black = black;
    }

    public List<Card> getRed() {
        return red;
    }

    public void setRed(List<Card> red) {
        this.red = red;
    }
}
