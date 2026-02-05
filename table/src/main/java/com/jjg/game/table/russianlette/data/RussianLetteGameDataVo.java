package com.jjg.game.table.russianlette.data;


import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.russianlette.message.resp.RussianLetteHistoryBean;
import com.jjg.game.table.russianlette.message.resp.RussianLetteSettlementInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 俄罗斯转盘的临时房间数据
 *
 * @author lhc
 */
public class RussianLetteGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private RussianLetteSettlementInfo russianLetteSettlementInfo;
    // 每次赢的历史记录
    private final List<RussianLetteHistoryBean> winAreaCfgIdHistory = new ArrayList<>();

    public RussianLetteGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    @Override
    public void clearRoundData(AbstractGameController<?, ?> gameController) {
        super.clearRoundData(gameController);
        russianLetteSettlementInfo = null;
    }

    public List<RussianLetteHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(RussianLetteHistoryBean russianLetteHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(russianLetteHistoryBean);
    }

    public RussianLetteSettlementInfo getAnimalsSettlementInfo() {
        return russianLetteSettlementInfo;
    }

    public void setAnimalsSettlementInfo(RussianLetteSettlementInfo russianLetteSettlementInfo) {
        this.russianLetteSettlementInfo = russianLetteSettlementInfo;
    }
}
