package com.jjg.game.poker.game.texas.room.data;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementInfo;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 本轮下注
     */
    private Map<Long, Long> roundBet = new HashMap<>();

    /**
     * 本局游戏定时器的id
     */
    private int timerId;
    /**
     * 结算信息
     */
    private NotifyTexasSettlementInfo notifyTexasSettlementInfo;
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


    /**
     * 获取当前的历史记录轮信息
     */
    public TexasHistoryRoundInfo getHistoryRoundInfo() {
        return texasHistory.getTexasHistoryRoundInfos().get(getRound() - 1);
    }

    public Map<Long, Long> getRoundBet() {
        return roundBet;
    }

    public void setRoundBet(Map<Long, Long> roundBet) {
        this.roundBet = roundBet;
    }

    public List<TexasSaveHistory> getTexasHistoryList() {
        return texasHistoryList;
    }

    public NotifyTexasSettlementInfo getNotifyTexasSettlementInfo() {
        return notifyTexasSettlementInfo;
    }

    public void setNotifyTexasSettlementInfo(NotifyTexasSettlementInfo notifyTexasSettlementInfo) {
        this.notifyTexasSettlementInfo = notifyTexasSettlementInfo;
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

    public List<Long> getPotValueList() {
        if (CollectionUtil.isNotEmpty(pool)) {
            return null;
        }
        return pool.stream().map(Pot::getAmount)
                .collect(Collectors.toList());
    }

    public int getSettlement() {
        return settlement;
    }

    public void setSettlement(int settlement) {
        this.settlement = settlement;
    }

    @Override
    public int getPoolId() {
        return TexasDataHelper.getPoolId(this);
    }

    @Override
    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        super.resetData(controller);
        this.notifyTexasSettlementInfo = null;
        this.pool.clear();
        this.texasHistory = null;
        this.maxBetValue = 0;
        this.roundBet.clear();
        this.settlement = 0;
    }
}
