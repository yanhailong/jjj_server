package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.ploy.data.PlayerSinglePloyGameData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class HilloPloyGameData extends PlayerSinglePloyGameData {
    // 当前局状态：有 currentCardId 表示玩家还有一局未结束。
    private int currentCardId;
    private long currentCoin;
    private int successTimes;
    private int skipTimes;
    private int currentBetMode;
    private long currentRoundStartTime;

    // 自动投注配置。betTimes=0 用 autoInfiniteBet 表示无限局，避免和“剩余 0 局”混淆。
    private boolean autoBetting;
    private boolean autoInfiniteBet;
    private long autoBet;
    private int autoGuessTimes;
    private int autoRemainBetTimes;

    // history 记录当前局过程；totalHistories 只保存已经结束并归档的局。
    private List<HilloHistoryInfo> history;
    private List<HilloHistory> totalHistories;

    public boolean hasActiveGame() {
        return currentCardId > 0;
    }

    public int getCurrentCardId() {
        return currentCardId;
    }

    public void setCurrentCardId(int currentCardId) {
        this.currentCardId = currentCardId;
    }

    public long getCurrentCoin() {
        return currentCoin;
    }

    public void setCurrentCoin(long currentCoin) {
        this.currentCoin = currentCoin;
    }

    public int getSuccessTimes() {
        return successTimes;
    }

    public void setSuccessTimes(int successTimes) {
        this.successTimes = successTimes;
    }

    public int getSkipTimes() {
        return skipTimes;
    }

    public void setSkipTimes(int skipTimes) {
        this.skipTimes = skipTimes;
    }

    public int getCurrentBetMode() {
        return currentBetMode;
    }

    public void setCurrentBetMode(int currentBetMode) {
        this.currentBetMode = currentBetMode;
    }

    public long getCurrentRoundStartTime() {
        return currentRoundStartTime;
    }

    public void setCurrentRoundStartTime(long currentRoundStartTime) {
        this.currentRoundStartTime = currentRoundStartTime;
    }

    public boolean isAutoBetting() {
        return autoBetting;
    }

    public void setAutoBetting(boolean autoBetting) {
        this.autoBetting = autoBetting;
    }

    public boolean isAutoInfiniteBet() {
        return autoInfiniteBet;
    }

    public void setAutoInfiniteBet(boolean autoInfiniteBet) {
        this.autoInfiniteBet = autoInfiniteBet;
    }

    public long getAutoBet() {
        return autoBet;
    }

    public void setAutoBet(long autoBet) {
        this.autoBet = autoBet;
    }

    public int getAutoGuessTimes() {
        return autoGuessTimes;
    }

    public void setAutoGuessTimes(int autoGuessTimes) {
        this.autoGuessTimes = autoGuessTimes;
    }

    public int getAutoRemainBetTimes() {
        return autoRemainBetTimes;
    }

    public void setAutoRemainBetTimes(int autoRemainBetTimes) {
        this.autoRemainBetTimes = autoRemainBetTimes;
    }

    public List<HilloHistoryInfo> getHistory() {
        return history;
    }

    public void setHistory(List<HilloHistoryInfo> history) {
        this.history = history;
    }

    public List<HilloHistory> getTotalHistories() {
        return totalHistories;
    }

    public void setTotalHistories(List<HilloHistory> totalHistories) {
        this.totalHistories = totalHistories;
    }

    public void addHistory(int cardId, int chooseId, String odd, int resultCardId) {
        if (history == null) {
            history = new ArrayList<>();
        }
        HilloHistoryInfo info = new HilloHistoryInfo(cardId, chooseId, odd);
        info.setResultCardId(resultCardId);
        history.add(info);
    }

    public void addSkipHistory(int cardId) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(HilloHistoryInfo.skipped(cardId));
    }

    public void decreaseAutoRemainBetTimes() {
        if (autoRemainBetTimes > 0) {
            autoRemainBetTimes--;
        }
    }

    // 只清自动投注配置，不清当前局；取消自动后未结束的局仍可由手动流程继续处理。
    public void clearAutoBet() {
        autoBetting = false;
        autoInfiniteBet = false;
        autoBet = 0;
        autoGuessTimes = 0;
        autoRemainBetTimes = 0;
    }

    public void addTotalHistory(HilloHistory totalHistory) {
        if (totalHistories == null) {
            totalHistories = new ArrayList<>();
        }
        totalHistories.add(totalHistory);
        int removeNum = totalHistories.size() - HilloConstant.Common.MAX_RECORD_NUM;
        while (removeNum > 0) {
            totalHistories.removeFirst();
            removeNum--;
        }
    }
}
