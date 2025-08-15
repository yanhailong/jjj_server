package com.jjg.game.table.birdsanimals.data;


import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.birdsanimals.message.AnimalsHistoryBean;
import com.jjg.game.table.birdsanimals.message.AnimalsSettlementInfo;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 飞禽走兽的临时房间数据
 *
 * @author 2CL
 */
public class AnimalsGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private AnimalsSettlementInfo animalsSettlementInfo;
    // 每次赢的历史记录
    private final List<AnimalsHistoryBean> winAreaCfgIdHistory = new ArrayList<>();

    public AnimalsGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<AnimalsHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(AnimalsHistoryBean animalsHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(animalsHistoryBean);
    }

    public AnimalsSettlementInfo getAnimalsSettlementInfo() {
        return animalsSettlementInfo;
    }

    public void setAnimalsSettlementInfo(AnimalsSettlementInfo animalsSettlementInfo) {
        this.animalsSettlementInfo = animalsSettlementInfo;
    }
}
