package com.jjg.game.table.luxurycarclub.data;


import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.luxurycarclub.message.LuxuryCarClubSettlementInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 豪车俱乐部的临时房间数据
 *
 * @author 2CL
 */
public class LuxuryCarClubGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private LuxuryCarClubSettlementInfo luxuryCarClubSettlementInfo;
    // 每次赢的历史记录
    private final List<Integer> winAreaCfgIdHistory = new ArrayList<>();

    public LuxuryCarClubGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<Integer> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(int posId) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(posId);
    }

    public LuxuryCarClubSettlementInfo getAnimalsSettlementInfo() {
        return luxuryCarClubSettlementInfo;
    }

    public void setAnimalsSettlementInfo(LuxuryCarClubSettlementInfo luxuryCarClubSettlementInfo) {
        this.luxuryCarClubSettlementInfo = luxuryCarClubSettlementInfo;
    }
}
