package com.jjg.game.poker.game.texas.room.data;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.message.bean.TexasHistory;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 德州扑克
 */
public class TexasGameDataVo extends BasePokerGameDataVo {
    /**
     * 必须初始化的参数是房间配置RoomCfg，如果后续子类添加数据需要在自己的构造函数中添加
     */
    public TexasGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    /**
     * 结算类型 0默认 1弃牌只剩1人 2全all
     */
    private int settlement;

    /**
     * 奖池
     */
    private List<Pot> pool = new ArrayList<>();
    /**
     * 庄家座位号
     */
    private int dealerSeatId = 0;

    /**
     * 庄家座在执行列表中的索引
     */
    private int dealerIndex = 0;

    /**
     * 本轮最大下注金额
     */
    private long maxBetValue;

    /**
     * 本局游戏定时器的id
     */
    private int timerId;

    /**
     * 临时gold列表
     */
    private final Map<Long, Long> tempGold = new HashMap<>();

    /**
     * 房间历史记录
     */
    private List<TexasSaveHistory> texasHistoryList = new ArrayList<>();

    /**
     * 本局历史记录
     */
    private TexasSaveHistory texasHistory;

    public List<TexasSaveHistory> getTexasHistoryList() {
        return texasHistoryList;
    }

    public void setTexasHistoryList(List<TexasSaveHistory> texasHistoryList) {
        this.texasHistoryList = texasHistoryList;
    }

    public TexasSaveHistory getTexasHistory() {
        return texasHistory;
    }

    public void setTexasHistory(TexasSaveHistory texasHistory) {
        this.texasHistory = texasHistory;
    }

    public Map<Long, Long> getTempGold() {
        return tempGold;
    }

    public int getTimerId() {
        return timerId;
    }

    public void addTimerId() {
        timerId++;
    }

    public long getMaxBetValue() {
        return maxBetValue;
    }

    public void setMaxBetValue(long maxBetValue) {
        this.maxBetValue = maxBetValue;
    }

    public int getDealerSeatId() {
        return dealerSeatId;
    }

    public void setDealerSeatId(int dealerSeatId) {
        this.dealerSeatId = dealerSeatId;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public void setDealerIndex(int dealerIndex) {
        this.dealerIndex = dealerIndex;
    }

    public List<Pot> getPool() {
        return pool;
    }

    public void setPool(List<Pot> pool) {
        this.pool = pool;
    }

    public int getSettlement() {
        return settlement;
    }

    public void setSettlement(int settlement) {
        this.settlement = settlement;
    }

    @Override
    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        super.resetData(controller);
        this.pool.clear();
        this.texasHistory = null;
        this.maxBetValue = 0;
        this.settlement = 0;
    }
}
