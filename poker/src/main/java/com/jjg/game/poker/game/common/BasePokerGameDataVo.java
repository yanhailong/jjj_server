package com.jjg.game.poker.game.common;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifySettlementInfo;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;

/**
 * @author lm
 * @date 2025/7/26 10:07
 */
public class BasePokerGameDataVo extends GameDataVo<Room_ChessCfg> {
    /**
     * 必须初始化的参数是房间配置RoomCfg，如果后续子类添加数据需要在自己的构造函数中添加
     */
    public BasePokerGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    @Override
    public void reloadRoomCfg() {
        roomCfg = GameDataManager.getRoom_ChessCfg(roomCfg.getId());
    }


    /**
     * 本轮游戏id
     */
    private int id;

    //座位id->座位状态
    private final TreeMap<Integer, SeatInfo> seatInfo = new TreeMap<>();
    /**
     * 当前需要操作的玩家索引
     */
    private int index;

    /**
     * 执行列表
     */
    private final List<PlayerSeatInfo> playerSeatInfoList = new ArrayList<>();

    /**
     * 公牌
     */
    private List<Integer> publicCards;

    /**
     * 游戏内牌组
     */
    private List<Integer> cards;

    /**
     * 执行列表执行的轮次
     */
    private int round = 1;
    /**
     * 基础下注信息
     */
    private final Map<Long, Long> baseBetInfo = new HashMap<>();
    /**
     * 结算信息
     */
    private NotifySettlementInfo notifySettlementInfo;


    public TreeMap<Integer, SeatInfo> getSeatInfo() {
        return seatInfo;
    }


    public int getId() {
        return id;
    }

    public void addId() {
        this.id += 1;
    }

    public NotifySettlementInfo getNotifySettlementInfo() {
        return notifySettlementInfo;
    }

    public void setNotifySettlementInfo(NotifySettlementInfo notifySettlementInfo) {
        this.notifySettlementInfo = notifySettlementInfo;
    }

    /**
     * 获取已经坐下的玩家人数
     */
    public int getSeatDownNum() {
        return (int) seatInfo.values()
                .stream()
                .filter(SeatInfo::isSeatDown)
                .count();
    }

    public Map<Long, Long> getBaseBetInfo() {
        return baseBetInfo;
    }

    public int getRound() {
        return round;
    }

    public void nextRound() {
        this.round++;
    }

    public List<PlayerSeatInfo> getPlayerSeatInfoList() {
        return playerSeatInfoList;
    }

    public PlayerSeatInfo getCurrentPlayerSeatInfo() {
        return playerSeatInfoList.get(index);
    }

    public boolean canStartGame() {
        int seatDownNum = getSeatDownNum();
        return seatDownNum >= getRoomCfg().getMinPlayer() && seatDownNum <= getRoomCfg().getMaxPlayer();
    }

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }


    public List<Integer> getPublicCards() {
        return publicCards;
    }

    public void setPublicCards(List<Integer> publicCards) {
        this.publicCards = publicCards;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void resetData() {
        this.id++;
        this.publicCards = null;
        this.round = 1;
        this.baseBetInfo.clear();
        this.notifySettlementInfo = null;
    }
}
