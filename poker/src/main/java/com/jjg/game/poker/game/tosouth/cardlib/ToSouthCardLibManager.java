package com.jjg.game.poker.game.tosouth.cardlib;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.poker.game.common.cardlib.AbstractCardLibManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 南方前进牌库管理器
 * 通用能力（水池控制 / 模型选择 / 分区 key / 生成模板）全部由 {@link AbstractCardLibManager} 提供。
 * 本类只保留南方前进特有的逻辑：
 * <ul>
 *     <li>GAME_TYPE 常量</li>
 *     <li>DAO 注入</li>
 *     <li>生成前预加载 {@link SouthernMoneyCfg}</li>
 *     <li>单局模拟委托给 {@link ToSouthCardLibGenerator}</li>
 *     <li>玩家连胜/盈亏的委托方法</li>
 * </ul>
 */
@Component
public class ToSouthCardLibManager
        extends AbstractCardLibManager<ToSouthCardLib, ToSouthCardLibDao> {

    /** 南方前进游戏 ID */
    private static final int GAME_TYPE_TO_SOUTH = CoreConst.GameType.TO_SOUTH;

    @Autowired
    private ToSouthCardLibDao cardLibDao;

    // 生成期缓存，避免每局重复查表
    private volatile SouthernMoneyCfg cachedMoneyCfg;
    private volatile int cachedPoolId;

    @Override
    protected int getGameType() {
        return GAME_TYPE_TO_SOUTH;
    }

    @Override
    protected ToSouthCardLibDao getCardLibDao() {
        return cardLibDao;
    }

    @Override
    protected boolean prepareGeneration() {
        SouthernMoneyCfg moneyCfg = getFirstSouthernMoneyCfg();
        if (moneyCfg == null) {
            log.error("缺少 SouthernMoneyCfg 配置");
            return false;
        }
        this.cachedMoneyCfg = moneyCfg;
        this.cachedPoolId = moneyCfg.getPoolId();
        return true;
    }

    @Override
    protected List<ToSouthCardLib> simulateOneGame() {
        if (cachedMoneyCfg == null) {
            return Collections.emptyList();
        }
        return ToSouthCardLibGenerator.simulateOneGame(cachedPoolId, cachedMoneyCfg);
    }

    /**
     * 获取第一条 SouthernMoneyCfg 配置（用于牌库生成）
     */
    private SouthernMoneyCfg getFirstSouthernMoneyCfg() {
        List<SouthernMoneyCfg> list = GameDataManager.getSouthernMoneyCfgList();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    // ==================== 玩家统计数据（持久化到 Redis） ====================

    /**
     * 获取玩家连赢/连输值
     */
    public int getPlayerWinStreak(long playerId) {
        return cardLibDao.getPlayerWinStreak(playerId);
    }

    /**
     * 设置玩家连赢/连输值
     */
    public void setPlayerWinStreak(long playerId, int streak) {
        cardLibDao.setPlayerWinStreak(playerId, streak);
    }

    /**
     * 获取玩家总盈亏
     */
    public long getPlayerTotalProfit(long playerId) {
        return cardLibDao.getPlayerTotalProfit(playerId);
    }

    /**
     * 累加玩家总盈亏
     */
    public void addPlayerTotalProfit(long playerId, long delta) {
        cardLibDao.addPlayerTotalProfit(playerId, delta);
    }
}
