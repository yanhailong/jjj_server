package com.jjg.game.slots.data;

import org.springframework.data.annotation.Id;

import java.util.*;

/**
 * slots 结果库
 *
 * @author 11
 * @date 2025/7/2 11:46
 */
public class SlotsResultLib<T extends AwardLineInfo> implements Cloneable {
    @Id
    protected String id;
    //游戏类型
    protected int gameType;
    //库的类型，在 SpecialResultLib的TypeProp字段中的类型
    protected Set<Integer> libTypeSet;
    //滚轴模式
    protected int rollerMode;
    //图标集合
    protected int[] iconArr;
    //总的中奖倍率
    protected long times;
    //中奖线信息
    protected List<T> awardLineInfoList;
    //修改格子后存储的信息
    protected List<SpecialGirdInfo> specialGirdInfoList;
    //小游戏奖励信息
    protected List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList;
    //本次触发的jackpotId
    protected List<Integer> jackpotIds;

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
        if (this.libTypeSet == null) {
            this.libTypeSet = new HashSet<>();
        }
        this.libTypeSet.add(libType);
    }

    public void delLibType(int libType) {
        if (this.libTypeSet == null) {
            return;
        }
        this.libTypeSet.remove(libType);
    }

    public List<SpecialGirdInfo> getSpecialGirdInfoList() {
        return specialGirdInfoList;
    }

    public void setSpecialGirdInfoList(List<SpecialGirdInfo> specialGirdInfoList) {
        this.specialGirdInfoList = specialGirdInfoList;
    }

    public void addSpecialGirdInfo(SpecialGirdInfo specialGirdInfo) {
        if (this.specialGirdInfoList == null) {
            this.specialGirdInfoList = new ArrayList<>();
        }
        this.specialGirdInfoList.add(specialGirdInfo);
    }

    public List<SpecialAuxiliaryInfo> getSpecialAuxiliaryInfoList() {
        return specialAuxiliaryInfoList;
    }

    public void setSpecialAuxiliaryInfoList(List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList) {
        this.specialAuxiliaryInfoList = specialAuxiliaryInfoList;
    }

    public void addSpecialAuxiliaryInfo(SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (this.specialAuxiliaryInfoList == null) {
            this.specialAuxiliaryInfoList = new ArrayList<>();
        }
        this.specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
    }

    public void addSpecialAuxiliaryInfo(List<SpecialAuxiliaryInfo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        if (this.specialAuxiliaryInfoList == null) {
            this.specialAuxiliaryInfoList = new ArrayList<>();
        }
        this.specialAuxiliaryInfoList.addAll(list);
    }

    public void addAwardLineInfo(T awardLineInfo) {
        if (this.awardLineInfoList == null) {
            this.awardLineInfoList = new ArrayList<>();
        }
        this.awardLineInfoList.add(awardLineInfo);
    }

    public void addAllAwardLineInfo(List<T> awardLineInfos) {
        if (awardLineInfos == null || awardLineInfos.isEmpty()) {
            return;
        }
        if (this.awardLineInfoList == null) {
            this.awardLineInfoList = new ArrayList<>();
        }
        this.awardLineInfoList.addAll(awardLineInfos);
    }

    public List<Integer> getJackpotIds() {
        return jackpotIds;
    }

    public void setJackpotIds(List<Integer> jackpotIds) {
        this.jackpotIds = jackpotIds;
    }

    public int firstJackpotId() {
        if (this.jackpotIds == null || this.jackpotIds.isEmpty()) {
            return 0;
        }
        return this.jackpotIds.get(0);
    }

    public void addJackpotId(int jackpotId) {
        if (this.jackpotIds == null) {
            this.jackpotIds = new ArrayList<>();
        }

        if (jackpotId < 1 || jackpotIds.contains(jackpotId)) {
            return;
        }
        this.jackpotIds.add(jackpotId);
    }

    public boolean containsJackpotId(int jackpotId) {
        if (this.jackpotIds == null || this.jackpotIds.isEmpty()) {
            return false;
        }
        return this.jackpotIds.contains(jackpotId);
    }

    public boolean jackpotEmpty() {
        return this.jackpotIds == null || this.jackpotIds.isEmpty();
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

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
