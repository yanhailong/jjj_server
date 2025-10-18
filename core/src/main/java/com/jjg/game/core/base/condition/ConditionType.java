package com.jjg.game.core.base.condition;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.base.condition.check.*;
import com.jjg.game.core.dao.CountDao;

import java.util.List;

/**
 * @author lm
 * @date 2025/10/16 17:41
 */
public enum ConditionType {
    //等级条件
    PLAYER_LEVEL(1, new PlayerLevelCheck()),
    //开服天数
    OPEN_SERVER_DAY(2, null),
    //个人投注次数
    BET_COUNT(10001, new PlayerBetCountCheck()),
    //个人游戏次数
    PLAY_GAME_COUNT(10002, new PlayerGameCountCheck()),
    //个人游戏实际赢钱
    PLAY_GAME_WIN_MONEY(10003, new PlayerGameWinMoneyCheck()),
    //个人单次充值
    PLAYER_PAY(11001, new PlayerRechargeCheck()),
    //个人累计充值
    PLAYER_SUM_PAY(11002, new PlayerTotalRechargeCheck()),
    //个人有效下注
    PLAYER_BET_ALL(12001, new PlayerEffectiveBetAllCheck()),
    //个人有效下注
    PLAYER_BET(12002, new PlayerSystemEffectiveBetAllCheck()),
    //指定游戏掉落
    PLAY_GAME(12003, new PlayerEffectiveBetDropCheck(12003)),
    //不在指定游戏掉落
    NOT_PLAY_GAME(12004, new PlayerEffectiveBetDropCheck(12004)),
    //指定游戏类型掉落
    PLAY_GAME_TYPE(12005, new PlayerEffectiveBetDropCheck(12005)),
    //指定房间倍场类型掉落
    PLAY_ROOM_TYPE(12006, new PlayerEffectiveBetDropCheck(12006)),
    //累积使用道具数量
    PLAY_USE_ITEM(12101, new PlayUseItemCheck()),
    ;

    /**
     * 具体条件检查类
     */
    private final ConditionCheck conditionCheck;
    /**
     * condition.xlsx 表id
     */
    private final int id;

    ConditionType(int id, ConditionCheck conditionCheck) {
        this.conditionCheck = conditionCheck;
        this.id = id;
    }

    public ConditionCheck getConditionCheck() {
        return conditionCheck;
    }

    public long addProgress(Object paramObject, List<Integer> cfg) {
        Object conditionObject = conditionCheck.analysisCondition(cfg);
        if (conditionObject == null) {
            return 0;
        }
        return conditionCheck.addProgress(paramObject, conditionObject);
    }

    public boolean doCheck(Object paramObject, List<Integer> cfg) {

        Object condition = conditionCheck.analysisCondition(cfg);
        if (condition == null) {
            return false;
        }
        return conditionCheck.check(paramObject, condition);
    }

    public long getProgress(Object paramObject) {
        return conditionCheck.getProgress(paramObject);
    }

    public int getId() {
        return id;
    }

    /**
     * 初始化数据主要是为了装载dao
     */
    public static void initData() {
        CountDao countDao = CommonUtil.getContext().getBean(CountDao.class);
        for (ConditionType conditionType : values()) {
            if (conditionType.getConditionCheck() instanceof BaseCheck check) {
                check.setCountDao(countDao);
            }
        }
    }

    /**
     * 条件功能枚举
     */
    public enum FunctionType {
        //活动
        ACTIVITY
    }
}
