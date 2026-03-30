package com.jjg.game.slots.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/8/30 11:45
 */
public class SpecialAuxiliaryAwardInfo {
    private int randCount;
    //奖励c
    private List<Integer> awardCList;

    public int getRandCount() {
        return randCount;
    }

    public void setRandCount(int randCount) {
        this.randCount = randCount;
    }

    public List<Integer> getAwardCList() {
        return awardCList;
    }

    public void setAwardCList(List<Integer> awardCList) {
        this.awardCList = awardCList;
    }

    public void addAwardC(int awardC) {
        if(this.awardCList == null) {
            this.awardCList = new ArrayList<>();
        }
        this.awardCList.add(awardC);
    }
}
