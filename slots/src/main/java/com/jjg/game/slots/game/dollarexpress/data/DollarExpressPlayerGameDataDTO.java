package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataIndexedDTO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 11
 * @date 2025/8/5 14:10
 */
@Document
public class DollarExpressPlayerGameDataDTO extends SlotsPlayerGameDataIndexedDTO {
    //累计的美钞数量
    private int totalDollars;
    //记录出现可收集美元的局数
    private int addDollarsCount;
    //记录收集美元时的押注总和(用于计算累计的美钞的平均值)
    private int addDollarsTotalStake;
    //是否可以投资
    private boolean invers;
    //已经选择的地区
    private Set<Integer> selectedAreaSet;
    //全地图解锁
    private boolean allUnLock;

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
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

    public boolean isInvers() {
        return invers;
    }

    public void setInvers(boolean invers) {
        this.invers = invers;
    }

    public Set<Integer> getSelectedAreaSet() {
        return selectedAreaSet;
    }

    public void setSelectedAreaSet(Set<Integer> selectedAreaSet) {
        this.selectedAreaSet = selectedAreaSet;
    }

    public boolean isAllUnLock() {
        return allUnLock;
    }

    public void setAllUnLock(boolean allUnLock) {
        this.allUnLock = allUnLock;
    }

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T data = super.converToGameData(cla);
        DollarExpressPlayerGameData dollarGameData = (DollarExpressPlayerGameData) data;
        dollarGameData.setInvers(new AtomicBoolean(this.invers));
        dollarGameData.setAllUnLock(new AtomicBoolean(this.allUnLock));
        return data;
    }
}
