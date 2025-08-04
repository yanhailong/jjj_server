package com.jjg.game.table.sizedicetreasure.data;

import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.sizedicetreasure.message.SizeDiceTreasureHistoryBean;
import com.jjg.game.table.sizedicetreasure.message.SizeDiceTreasureSettlementInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 大小骰宝的临时房间数据
 *
 * @author 2CL
 */
public class SizeDiceTreasureGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private SizeDiceTreasureSettlementInfo sizeDiceTreasureSettlementInfo;
    // 每次赢的历史记录
    private final List<SizeDiceTreasureHistoryBean> winAreaCfgIdHistory = new ArrayList<>();

    public SizeDiceTreasureGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<SizeDiceTreasureHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(SizeDiceTreasureHistoryBean sizeDiceTreasureHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(sizeDiceTreasureHistoryBean);
    }

    public SizeDiceTreasureSettlementInfo getAnimalsSettlementInfo() {
        return sizeDiceTreasureSettlementInfo;
    }

    public void setAnimalsSettlementInfo(SizeDiceTreasureSettlementInfo sizeDiceTreasureSettlementInfo) {
        this.sizeDiceTreasureSettlementInfo = sizeDiceTreasureSettlementInfo;
    }
}
