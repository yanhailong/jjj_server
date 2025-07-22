package com.jjg.game.table.baccarat.data;

import com.jjg.game.core.data.Player;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.message.resp.BaccaratCardState;
import com.jjg.game.table.baccarat.message.resp.NotifyBaccaratSettlementInfo;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 百家乐游戏数据
 *
 * @author 2CL
 */
public class BaccaratGameDataVo extends TableGameDataVo {

    // 牌ID
    private ArrayList<Byte> cardList = new ArrayList<>();
    // 初始牌数量
    private int initCardNum = 0;
    // 保存当前场上的路单数据, 输赢状态和牌型状态
    private final List<BaccaratCardState> betRecord = new ArrayList<>();
    // 是否补牌了
    private boolean isFillCard;
    // 保存场上的结算信息,如果有玩家中途加入才可以看见结算数据
    private NotifyBaccaratSettlementInfo baccaratSettlementInfo;

    public BaccaratGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    @Override
    public void clearRoundData() {
        super.clearRoundData();
        isFillCard = false;
        // 将玩家的座位复位
        for (GamePlayer gamePlayer : gamePlayerMap.values()) {
            gamePlayer.getTableGameData().setSitNum(0);
        }
        List<GamePlayer> gamePlayers =
            // 取金币最高的6个人，放在场上
            gamePlayerMap.values()
                .stream()
                .sorted(Comparator.comparingLong(Player::getGold).reversed()).toList()
                .subList(0, Math.min(gamePlayerMap.size(), 6));
        // 将前6个人的位置进行排序
        for (int i = 1; i <= gamePlayers.size(); i++) {
            GamePlayer gamePlayer = gamePlayers.get(i - 1);
            gamePlayer.getTableGameData().setSitNum(i);
        }
        // 结算信息清除
        baccaratSettlementInfo = null;
    }

    public ArrayList<Byte> getCardList() {
        return cardList;
    }

    public void setCardList(ArrayList<Byte> cardList) {
        this.cardList = cardList;
    }

    public List<BaccaratCardState> getBetRecord() {
        return betRecord;
    }

    public int getInitCardNum() {
        return initCardNum;
    }

    public void setInitCardNum(int initCardNum) {
        this.initCardNum = initCardNum;
    }

    public boolean isFillCard() {
        return isFillCard;
    }

    public void setFillCard(boolean fillCard) {
        isFillCard = fillCard;
    }

    public NotifyBaccaratSettlementInfo getBaccaratSettlementInfo() {
        return baccaratSettlementInfo;
    }

    public void setBaccaratSettlementInfo(NotifyBaccaratSettlementInfo baccaratSettlementInfo) {
        this.baccaratSettlementInfo = baccaratSettlementInfo;
    }
}
