package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DollarExpress房间模式玩家数据
 * @author lm
 * @date 2026/2/4
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document
public class DollarExpressPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
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
