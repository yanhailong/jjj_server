package com.jjg.game.slots.data;

import org.springframework.data.annotation.Id;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * slots 结果库
 * @author 11
 * @date 2025/7/2 11:46
 */
public class SlotsResultLib<T extends AwardLineInfo> implements Cloneable{
    @Id
    protected String id;
    //游戏类型
    protected int gameType;
    //库的类型，在 SpecialResultLib的TypeProp字段中的类型
    protected Set<Integer> libTypeSet;
    //滚轴模式
    protected int rollerMode;
    //滚轴id
    protected int rollerId;
    //图标集合
    protected int[] iconArr;
    //总的中奖倍率
    protected long times;
    //中奖线信息
    protected List<T> awardLineInfoList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public Set<Integer> getLibTypeSet() {
        return libTypeSet;
    }

    public void setLibTypeSet(Set<Integer> libTypeSet) {
        this.libTypeSet = libTypeSet;
    }

    public int getRollerMode() {
        return rollerMode;
    }

    public void setRollerMode(int rollerMode) {
        this.rollerMode = rollerMode;
    }

    public int getRollerId() {
        return rollerId;
    }

    public void setRollerId(int rollerId) {
        this.rollerId = rollerId;
    }

    public int[] getIconArr() {
        return iconArr;
    }

    public void setIconArr(int[] iconArr) {
        this.iconArr = iconArr;
    }

    public long getTimes() {
        return times;
    }

    public void setTimes(long times) {
        this.times = times;
    }

    public List<T> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<T> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }

    public void addTimes(long times) {
        this.times += times;
    }

    public void addLibType(int libType) {
        if(this.libTypeSet == null) {
            this.libTypeSet = new HashSet<>();
        }
        this.libTypeSet.add(libType);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SlotsResultLib<?> that = (SlotsResultLib<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
