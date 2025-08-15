package com.jjg.game.table.riveranimals.data;


import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.riveranimals.message.RiverAnimalsHistoryBean;
import com.jjg.game.table.riveranimals.message.RiverAnimalsSettlementInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 鱼虾蟹的临时房间数据
 *
 * @author 2CL
 */
public class RiverAnimalsGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private RiverAnimalsSettlementInfo riverAnimalsSettlementInfo;
    // 每次赢的历史记录
    private final List<RiverAnimalsHistoryBean> winAreaCfgIdHistory = new ArrayList<>();

    public RiverAnimalsGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<RiverAnimalsHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(RiverAnimalsHistoryBean riverAnimalsHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(riverAnimalsHistoryBean);
    }

    public RiverAnimalsSettlementInfo getAnimalsSettlementInfo() {
        return riverAnimalsSettlementInfo;
    }

    public void setAnimalsSettlementInfo(RiverAnimalsSettlementInfo riverAnimalsSettlementInfo) {
        this.riverAnimalsSettlementInfo = riverAnimalsSettlementInfo;
    }
}
