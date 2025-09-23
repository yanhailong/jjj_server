package com.jjg.game.slots.data;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/7/11 10:51
 */
public class SlotsPlayerGameData {
    protected PlayerController playerController;
    //游戏类型
    protected int gameType;
    //场次配置id
    protected int roomCfgId;
    //最近一次的模式id
    protected int lastModelId;
    //最近一次所在的区间
    protected int lastSectionIndex;
    //当前所处状态(美元快递) 0.正常  1.二选一  2.正在免费旋转
    protected int status;
    //最后一次活跃时间
    protected int lastActiveTime;
    //是否在线
    protected boolean online;
    //最近一次的押注(单线押分)
    protected long oneBetScore;
    //最近一次的押注(总押分)
    protected long allBetScore;
    //玩家累计押注金额
    protected long allBet;
    //玩家累计获得奖池(倍场)金额
    protected long rewardPoolGold;
    //玩家奖池(倍场)累计贡献金额金额(没有减去已获得金额)
    protected long contribtPoolGold;
    //是否玩过该slots游戏
    protected AtomicBoolean hasPlaySlots = new AtomicBoolean(false);
    //剩余的免费次数
    protected AtomicInteger remainFreeCount = new AtomicInteger(0);
    //当前的免费游戏数组中的下标值
    //比如总共中奖8次免费， 第一次应该取这8次结果中的的第一个，以此类推
    protected AtomicInteger freeIndex = new AtomicInteger(0);
    //缓存免费的结果库
    protected Object freeLib;
    //用于测试
    protected LinkedList<TestLibData> testLibDataList;


    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public AtomicBoolean getHasPlaySlots() {
        return hasPlaySlots;
    }

    public void setHasPlaySlots(AtomicBoolean hasPlaySlots) {
        this.hasPlaySlots = hasPlaySlots;
    }

    public long playerId(){
        return playerController.playerId();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(int lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getOneBetScore() {
        return oneBetScore;
    }

    public void setOneBetScore(long oneBetScore) {
        this.oneBetScore = oneBetScore;
    }

    public long getAllBetScore() {
        return allBetScore;
    }

    public void setAllBetScore(long allBetScore) {
        this.allBetScore = allBetScore;
    }

    public long getAllBet() {
        return allBet;
    }

    public void addAllBet(long bet) {
        this.allBet += bet;
    }

    public void setAllBet(long allBet) {
        this.allBet = allBet;
    }

    public long getRewardPoolGold() {
        return rewardPoolGold;
    }

    public void setRewardPoolGold(long rewardPoolGold) {
        this.rewardPoolGold = rewardPoolGold;
    }

    public long getContribtPoolGold() {
        return contribtPoolGold;
    }

    public void setContribtPoolGold(long contribtPoolGold) {
        this.contribtPoolGold = contribtPoolGold;
    }

    public void setRemainFreeCount(AtomicInteger remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    public AtomicInteger getRemainFreeCount() {
        return remainFreeCount;
    }

    /**
     * 获取玩家对奖池的累计贡献金额
     * @return
     */
    public long getAllContribtPoolGold() {
        return this.contribtPoolGold - this.rewardPoolGold;
    }

    public long addContribtPoolGold(long value){
        this.contribtPoolGold += value;
        return this.contribtPoolGold;
    }

    public AtomicInteger getFreeIndex() {
        return freeIndex;
    }

    public void setFreeIndex(AtomicInteger freeIndex) {
        this.freeIndex = freeIndex;
    }

    public Object getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(Object freeLib) {
        this.freeLib = freeLib;
    }

    public LinkedList<TestLibData> getTestLibDataList() {
        return testLibDataList;
    }

    public void setTestLibDataList(LinkedList<TestLibData> testLibDataList) {
        this.testLibDataList = testLibDataList;
    }

    public void addTestIconsData(TestLibData testLibData) {
        if(this.testLibDataList == null){
            this.testLibDataList = new LinkedList<>();
        }
        this.testLibDataList.add(testLibData);
    }

    public TestLibData pollTestLibData() {
        if(this.testLibDataList == null || this.testLibDataList.isEmpty()){
            return null;
        }
        return this.testLibDataList.poll();
    }

    public long addSmallPoolReward(long gold){
        this.rewardPoolGold += gold;
        return this.rewardPoolGold;
    }

    public int getLastModelId() {
        return lastModelId;
    }

    public void setLastModelId(int lastModelId) {
        this.lastModelId = lastModelId;
    }

    public int getLastSectionIndex() {
        return lastSectionIndex;
    }

    public void setLastSectionIndex(int lastSectionIndex) {
        this.lastSectionIndex = lastSectionIndex;
    }

    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception{
        Constructor<T> constructor = cla.getConstructor();
        T t = constructor.newInstance();
        BeanUtils.copyProperties(this,t);
        t.setPlayerId(this.playerId());
        t.setRemainFreeCount(this.remainFreeCount.get());
        t.setFreeIndex(this.freeIndex.get());
        return t;
    }
}
