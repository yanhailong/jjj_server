package com.jjg.game.table.vietnamdice.data;

import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.vietnamdice.message.VietnamDiceHistoryBean;
import com.jjg.game.table.vietnamdice.message.VietnamDiceSettlementInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 越南色碟的临时房间数据
 *
 * @author 2CL
 */
public class VietnamDiceGameDataVo extends TableGameDataVo {
    // 结算信息保存
    private VietnamDiceSettlementInfo vietnamDiceSettlementInfo;
    // 每次赢的历史记录
    private final List<VietnamDiceHistoryBean> winAreaCfgIdHistory = new ArrayList<>();

    public VietnamDiceGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    public List<VietnamDiceHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    public void addWinAreaCfgIdHistory(VietnamDiceHistoryBean vietnamDiceHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(vietnamDiceHistoryBean);
    }

    public VietnamDiceSettlementInfo getVietnamDiceSettlementInfo() {
        return vietnamDiceSettlementInfo;
    }

    public void setVietnamDiceSettlementInfo(VietnamDiceSettlementInfo vietnamDiceSettlementInfo) {
        this.vietnamDiceSettlementInfo = vietnamDiceSettlementInfo;
    }
}
