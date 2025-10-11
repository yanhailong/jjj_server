package com.jjg.game.poker.game.common;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.core.data.Room;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.timer.RoomTimerEvent;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/7/26 10:07
 */
public abstract class BasePokerGameDataVo extends GameDataVo<Room_ChessCfg> {
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
    private long id = 0;

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
     * 当前玩家定时器参数
     */
    private RoomTimerEvent<IProcessorHandler, Room> playerTimerEvent;

    /**
     * gm临时牌组
     */
    public List<Integer> tempCard;

    public List<Integer> getTempCard() {
        return tempCard;
    }

    public void setTempCard(List<Integer> tempCard) {
        this.tempCard = tempCard;
    }

    public RoomTimerEvent<IProcessorHandler, Room> getPlayerTimerEvent() {
        return playerTimerEvent;
    }

    public void setPlayerTimerEvent(RoomTimerEvent<IProcessorHandler, Room> playerTimerEvent) {
        this.playerTimerEvent = playerTimerEvent;
    }

    public TreeMap<Integer, SeatInfo> getSeatInfo() {
        return seatInfo;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }


    /**
     * 获取已经坐下的玩家人数
     */
    public int getSeatDownNum() {
        return (int) seatInfo.values()
                .stream()
                .filter(info -> {
                    GamePlayer gamePlayer = getGamePlayer(info.getPlayerId());
                    if (Objects.nonNull(gamePlayer)) {
                        return gamePlayer.getPokerPlayerGameData().isInit() && info.isSeatDown();
                    }
                    return false;
                }).count();
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
        if (index >= playerSeatInfoList.size()) {
            return null;
        }
        return playerSeatInfoList.get(index);
    }

    /**
     * 阶段是否正在结束处理中
     *
     * @return true 是 false否
     */
    public boolean isPhaseEnd() {
        return getPhaseEndTime() < System.currentTimeMillis() + 500;
    }

    /**
     * 是否能开启游戏
     */
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

    public Map<Long, PlayerSeatInfo> getPlayerSeatInfoMap() {
        return playerSeatInfoList.stream().collect(Collectors.toMap(PlayerSeatInfo::getPlayerId, info -> info));
    }

    public long getPlayerGameNnm() {
        return playerSeatInfoList.stream().filter(info -> !info.isDelState()).count();
    }

    public abstract int getPoolId();

    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        this.publicCards = null;
        this.round = TexasConstant.Common.INIT_ROUND;
        this.baseBetInfo.clear();
        if (Objects.nonNull(playerTimerEvent)) {
            controller.removePlayerTimerEvent(getPlayerTimerEvent());
        }
        cards = null;
        playerSeatInfoList.clear();
        this.playerTimerEvent = null;
    }
}
