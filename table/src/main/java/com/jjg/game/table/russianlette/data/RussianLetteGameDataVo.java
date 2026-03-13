package com.jjg.game.table.russianlette.data;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
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

    /** 结算阶段使用的最终结算信息（含玩家金币变化），用于断线重连恢复 */
    private RussianLetteSettlementInfo russianLetteSettlementInfo;

    /** 每次开奖的历史记录（最多保留 roomCfg.recordsNum 条） */
    private final List<RussianLetteHistoryBean> winAreaCfgIdHistory = new ArrayList<>();

    /**
     * 开奖阶段（DRAW_ON）生成的历史记录快照，供结算阶段使用
     * <p>包含本局中奖的 betAreaId 列表和骰子点数（diceData）</p>
     */
    private RussianLetteHistoryBean drawPhaseHistoryBean;

    /**
     * 开奖阶段（DRAW_ON）命中的获奖配置列表，供结算阶段进行金币计算
     * <p>由 DiceDataHolder.getWinPosWeightCfg() 返回，结算完成后可清除</p>
     */
    private List<WinPosWeightCfg> drawPhaseWinCfgs;

    /** GM 测试：指定下一局开奖结果（0-36），-1 表示不干预，使用后自动清除 */
    private int testDiceData = -1;

    public RussianLetteGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    @Override
    public void clearRoundData(AbstractGameController<?, ?> gameController) {
        super.clearRoundData(gameController);
        russianLetteSettlementInfo = null;
        drawPhaseHistoryBean = null;
        drawPhaseWinCfgs = null;
    }

    // ===================== winAreaCfgIdHistory =====================

    public List<RussianLetteHistoryBean> getWinAreaCfgIdHistory() {
        return winAreaCfgIdHistory;
    }

    /**
     * 添加一条开奖历史记录，超过 recordsNum 时滚动删除最旧记录
     */
    public void addWinAreaCfgIdHistory(RussianLetteHistoryBean russianLetteHistoryBean) {
        int recordsNum = roomCfg.getRecords_num();
        if (winAreaCfgIdHistory.size() >= recordsNum) {
            winAreaCfgIdHistory.remove(0);
        }
        winAreaCfgIdHistory.add(russianLetteHistoryBean);
    }

    // ===================== settlementInfo =====================

    public RussianLetteSettlementInfo getSettlementInfo() {
        return russianLetteSettlementInfo;
    }

    public void setSettlementInfo(RussianLetteSettlementInfo russianLetteSettlementInfo) {
        this.russianLetteSettlementInfo = russianLetteSettlementInfo;
    }

    // ===================== drawPhaseHistoryBean =====================

    public RussianLetteHistoryBean getDrawPhaseHistoryBean() {
        return drawPhaseHistoryBean;
    }

    public void setDrawPhaseHistoryBean(RussianLetteHistoryBean drawPhaseHistoryBean) {
        this.drawPhaseHistoryBean = drawPhaseHistoryBean;
    }

    // ===================== drawPhaseWinCfgs =====================

    public List<WinPosWeightCfg> getDrawPhaseWinCfgs() {
        return drawPhaseWinCfgs;
    }

    public void setDrawPhaseWinCfgs(List<WinPosWeightCfg> drawPhaseWinCfgs) {
        this.drawPhaseWinCfgs = drawPhaseWinCfgs;
    }

    // ===================== testDiceData (GM) =====================

    public int getTestDiceData() {
        return testDiceData;
    }

    public void setTestDiceData(int testDiceData) {
        this.testDiceData = testDiceData;
    }
}
