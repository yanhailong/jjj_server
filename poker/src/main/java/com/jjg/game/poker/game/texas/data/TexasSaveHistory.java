package com.jjg.game.poker.game.texas.data;

import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryRoundInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 德州扑克内存历史记录
 *
 * @author lm
 * @date 2025/8/7 10:16
 */
public class TexasSaveHistory {
    // 对局id
    private long id;
    // 小盲
    private long SBValue;
    // 大盲
    private long BBValue;
    //手牌信息
    private Map<Long, List<Integer>> allCards;
    //比牌信息
    private Map<Long, List<Integer>> settlementAllCards;
    // 第二轮公牌前端id
    private List<Integer> preFlop;
    // 第三轮公牌前端id
    private int thirdCardId;
    // 第四轮公牌前端id
    private int fourthCardId;
    // 本轮总获得的值
    private List<TexasHistoryPlayerInfo> totalPlayerBetInfo;
    // 轮次信息
    private List<TexasHistoryRoundInfo> texasHistoryRoundInfos;
    //最后池信息
    private List<Long> potList;

    public List<Long> getPotList() {
        return potList;
    }

    public void setPotList(List<Long> potList) {
        this.potList = potList;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSBValue() {
        return SBValue;
    }

    public void setSBValue(long SBValue) {
        this.SBValue = SBValue;
    }

    public long getBBValue() {
        return BBValue;
    }

    public void setBBValue(long BBValue) {
        this.BBValue = BBValue;
    }

    public Map<Long, List<Integer>> getAllCards() {
        return allCards;
    }

    public void setAllCards(Map<Long, List<Integer>> allCards) {
        this.allCards = allCards;
    }

    public Map<Long, List<Integer>> getSettlementAllCards() {
        return settlementAllCards;
    }

    public void setSettlementAllCards(Map<Long, List<Integer>> settlementAllCards) {
        this.settlementAllCards = settlementAllCards;
    }

    public List<Integer> getPreFlop() {
        return preFlop;
    }

    public void setPreFlop(List<Integer> preFlop) {
        this.preFlop = preFlop;
    }

    public int getThirdCardId() {
        return thirdCardId;
    }

    public void setThirdCardId(int thirdCardId) {
        this.thirdCardId = thirdCardId;
    }

    public int getFourthCardId() {
        return fourthCardId;
    }

    public void setFourthCardId(int fourthCardId) {
        this.fourthCardId = fourthCardId;
    }

    public List<TexasHistoryPlayerInfo> getTotalPlayerBetInfo() {
        return totalPlayerBetInfo;
    }

    public void setTotalPlayerBetInfo(List<TexasHistoryPlayerInfo> totalPlayerBetInfo) {
        this.totalPlayerBetInfo = totalPlayerBetInfo;
    }

    public Map<Long, TexasHistoryPlayerInfo> getTotalPlayerBetInfoMap() {
        return totalPlayerBetInfo.stream().collect(Collectors.toMap(info -> info.playerId, info -> info));
    }

    public List<TexasHistoryRoundInfo> getTexasHistoryRoundInfos() {
        return texasHistoryRoundInfos;
    }

    public void setTexasHistoryRoundInfos(List<TexasHistoryRoundInfo> texasHistoryRoundInfos) {
        this.texasHistoryRoundInfos = texasHistoryRoundInfos;
    }
}
