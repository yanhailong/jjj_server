package com.jjg.game.slots.game.dollarexpress.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/7/8 11:10
 */
public class Train {
    private int specialAuxiliaryId;
    //车厢
    private List<int[]> coachs;
    private int poolId;

    public int getSpecialAuxiliaryId() {
        return specialAuxiliaryId;
    }

    public void setSpecialAuxiliaryId(int specialAuxiliaryId) {
        this.specialAuxiliaryId = specialAuxiliaryId;
    }

    public List<int[]> getCoachs() {
        return coachs;
    }

    public void setCoachs(List<int[]> coachs) {
        this.coachs = coachs;
    }

    public int getPoolId() {
        return poolId;
    }

    public void setPoolId(int poolId) {
        this.poolId = poolId;
    }

    /**
     * 添加车厢信息
     * @param betType
     * @param times
     */
    public void addCoach(int betType,int times){
        if(this.coachs == null){
            this.coachs = new ArrayList<>();
        }
        this.coachs.add(new int[]{betType,times});
    }
}
