package com.jjg.game.table.dicetreasure.data;


import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.dicetreasure.message.DiceTreasureHistoryBean;
import com.jjg.game.table.dicetreasure.message.DiceTreasureSettlementInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 骰宝的临时房间数据
 *
 * @author 2CL
 */
public class DiceTreasureGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private DiceTreasureSettlementInfo diceTreasureSettlementInfo;
    // 每次赢的历史记录
    private final List<DiceTreasureHistoryBean> winAreaCfgIdHistory = new ArrayList<>();
    // gm结果
    private final List<Integer> gmResult = new ArrayList<>();

    public DiceTreasureGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<Integer> getGmResult() {
        return gmResult;
    }

    @Override
    public void clearRoundData(AbstractGameController<?, ?> gameController) {
        super.clearRoundData(gameController);
        diceTreasureSettlementInfo = null;
        gmResult.clear();
    }

    public List<DiceTreasureHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(DiceTreasureHistoryBean diceTreasureHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        int deleteType = roomCfg.getRecordDeleteType();
        // 覆盖
        if (deleteType == 0) {
            if (winAreaCfgIdHistory.size() >= recordsNum) {
                winAreaCfgIdHistory.removeFirst();
            }
        } else {
            winAreaCfgIdHistory.clear();
        }
        winAreaCfgIdHistory.add(diceTreasureHistoryBean);
    }

    public DiceTreasureSettlementInfo getAnimalsSettlementInfo() {
        return diceTreasureSettlementInfo;
    }

    public void setAnimalsSettlementInfo(DiceTreasureSettlementInfo diceTreasureSettlementInfo) {
        this.diceTreasureSettlementInfo = diceTreasureSettlementInfo;
    }
}
