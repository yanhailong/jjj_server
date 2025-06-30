package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家游戏数据
 * @author 11
 * @date 2025/6/10 18:07
 */
public class PlayerGameData {
    private static final Logger log = LoggerFactory.getLogger(PlayerGameData.class);

    private PlayerController playerController;
    //游戏类型
    private int gameType;
    //场次配置id
    private int wareId;
    //最近一次的押注
    private long lastBetValue;
    //特殊模式
    private int specialType;
    //最近一次走特殊中奖时记录的 resultShowId
    private int resultShowId;
    //免费模式-中金火车
    private boolean goldTrainInFree;
    //是否能选择免费游戏模式
    private AtomicBoolean canChooseFreeType = new AtomicBoolean(false);
    //剩余免费次数
    private AtomicInteger reaminFreeCount = new AtomicInteger(0);
    //出现美金的次数
    private AtomicInteger showDollarCount = new AtomicInteger(0);
    //出现美金的平均金额
    private BigDecimal showDollarValueAve = new BigDecimal(0);


    public PlayerGameData(PlayerController playerController) {
        this.playerController = playerController;
    }

    public PlayerController getPlayerController() {
        return playerController;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public void setWareId(int wareId) {
        this.wareId = wareId;
    }

    public int getWareId(){
        return this.wareId;
    }

    public long playerId(){
        return playerController.playerId();
    }

    public int getSpecialType() {
        return specialType;
    }

    public void setSpecialType(int specialType) {
        this.specialType = specialType;
    }

    public AtomicBoolean getCanChooseFreeType() {
        return canChooseFreeType;
    }

    public boolean isGoldTrainInFree() {
        return goldTrainInFree;
    }

    public void setGoldTrainInFree(boolean goldTrainInFree) {
        this.goldTrainInFree = goldTrainInFree;
    }

    public int addFreeCount(int count){
        return this.reaminFreeCount.addAndGet(count);
    }

    public void setFreeCount(int count){
        this.reaminFreeCount.set(count);
    }

    public int getResultShowId() {
        return resultShowId;
    }

    public void setResultShowId(int resultShowId) {
        this.resultShowId = resultShowId;
    }

    public long getLastBetValue() {
        return lastBetValue;
    }

    public void setLastBetValue(long lastBetValue) {
        this.lastBetValue = lastBetValue;
    }

    public AtomicInteger getShowDollarCount() {
        return showDollarCount;
    }


    public int addShowDollarCount(int count){
        return this.showDollarCount.addAndGet(count);
    }

    public void addShowDollarValue(long value){
        BigDecimal v = BigDecimal.valueOf(value);
        this.showDollarValueAve = this.showDollarValueAve.add(v).divide(DollarExpressConst.Common.BIGDECIMAL_TWO).setScale(0, RoundingMode.HALF_UP);
    }

    public long getShowDollarValueAve() {
        if(this.showDollarValueAve == null){
            return 0;
        }
        return this.showDollarValueAve.longValue();
    }
}
