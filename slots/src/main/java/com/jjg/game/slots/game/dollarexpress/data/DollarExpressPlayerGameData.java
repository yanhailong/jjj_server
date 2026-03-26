package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 玩家游戏数据
 *
 * @author 11
 * @date 2025/6/10 18:07
 */
@Document
public class DollarExpressPlayerGameData extends SlotsPlayerGameData {
    //累计的美钞数量
    private int totalDollars;
    //记录出现可收集美元的局数
    private int addDollarsCount;
    //记录收集美元时的押注总和(用于计算累计的美钞的平均值)
    private int addDollarsTotalStake;
    //是否可以投资
    private AtomicBoolean invers = new AtomicBoolean(false);
    //已经选择的地区
    private Set<Integer> selectedAreaSet;
    //全地图解锁
    private AtomicBoolean allUnLock = new AtomicBoolean(false);

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
    }

    public void addDollasCount(int count) {
        this.totalDollars += count;
    }

    public void addDollarsTotalStake(long stake) {
        this.addDollarsCount++;
        this.addDollarsTotalStake += (int) stake;
    }

    public AtomicBoolean getInvers() {
        return invers;
    }

    public void setInvers(AtomicBoolean invers) {
        this.invers = invers;
    }

    public Set<Integer> getSelectedAreaSet() {
        return selectedAreaSet;
    }

    public void setSelectedAreaSet(Set<Integer> selectedAreaSet) {
        this.selectedAreaSet = selectedAreaSet;
    }

    /**
     * 添加已选的地区
     *
     * @param areaId
     */
    public boolean addSelectedArea(int areaId) {
        if (this.selectedAreaSet == null) {
            this.selectedAreaSet = new HashSet<>();
        }
        return this.selectedAreaSet.add(areaId);
    }

    public boolean areaSelected(int areaId) {
        if (this.selectedAreaSet == null) {
            return false;
        }
        return this.selectedAreaSet.contains(areaId);
    }

    public boolean areaAllUnlock() {
        if (this.selectedAreaSet == null || this.selectedAreaSet.isEmpty()) {
            return false;
        }
        return this.selectedAreaSet.size() >= 8;
    }

    public int getAddDollarsCount() {
        return addDollarsCount;
    }

    public void setAddDollarsCount(int addDollarsCount) {
        this.addDollarsCount = addDollarsCount;
    }

    public int getAddDollarsTotalStake() {
        return addDollarsTotalStake;
    }

    public void setAddDollarsTotalStake(int addDollarsTotalStake) {
        this.addDollarsTotalStake = addDollarsTotalStake;
    }

    /**
     * 清除投资小游戏相关
     */
    public void clearInvers() {
        this.addDollarsCount = 0;
        this.addDollarsTotalStake = 0;
    }

    public AtomicBoolean getAllUnLock() {
        return allUnLock;
    }

    public void setAllUnLock(AtomicBoolean allUnLock) {
        this.allUnLock = allUnLock;
    }

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof DollarExpressPlayerGameDataDTO dollarDto) {
            dollarDto.setInvers(this.invers.get());
            dollarDto.setTotalDollars(this.totalDollars);
            dollarDto.setAddDollarsCount(this.addDollarsCount);
            dollarDto.setAddDollarsTotalStake(this.addDollarsTotalStake);
            dollarDto.setSelectedAreaSet(this.selectedAreaSet);
            dollarDto.setAllUnLock(this.allUnLock.get());
            dollarDto.setRemainFreeCount(this.getRemainFreeCount().get());
            dollarDto.setFreeIndex(this.getFreeIndex().get());
            if (freeLib instanceof DollarExpressResultLib lib) {
                dollarDto.setFreeLib(lib);
            }
        }
        if (dto instanceof DollarExpressPlayerGameDataRoomDTO dollarDto) {
            dollarDto.setInvers(this.invers.get());
            dollarDto.setTotalDollars(this.totalDollars);
            dollarDto.setAddDollarsCount(this.addDollarsCount);
            dollarDto.setAddDollarsTotalStake(this.addDollarsTotalStake);
            dollarDto.setSelectedAreaSet(this.selectedAreaSet);
            dollarDto.setAllUnLock(this.allUnLock.get());
            dollarDto.setRemainFreeCount(this.getRemainFreeCount().get());
            dollarDto.setFreeIndex(this.getFreeIndex().get());
            if (freeLib instanceof DollarExpressResultLib lib) {
                dollarDto.setFreeLib(lib);
            }
        }
        return dto;
    }
}
