package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.EnumUtil;
import com.jjg.game.core.base.condition.ConditionType;
import com.jjg.game.core.base.condition.check.record.BaseCheckCondition;
import com.jjg.game.core.base.condition.check.record.BaseCheckParam;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveCondition;
import com.jjg.game.core.base.condition.check.record.PlayerEffectiveParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 12003_每累计达到有效流水时触发|指定范围内游戏可多个游戏_指定范围内游戏可多个游戏
 * 12004_每累计达到有效流水时触发|排除指定范围内游戏可多个游戏
 * 12005_每累计达到有效流水时触发|指定范围内游戏类型可多个类型
 * 12005_每累计达到有效流水时触发|指定倍场游戏类型可多个倍场
 *
 * @author lm
 * @date 2025/10/17 10:11
 */
public class PlayerEffectiveBetDropCheck extends BaseCheck {
    private final int conditionTypeId;

    public PlayerEffectiveBetDropCheck(int conditionTypeId) {
        this.conditionTypeId = conditionTypeId;
    }

    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerEffectiveParam param && conditionObject instanceof PlayerEffectiveCondition condition) {
            if (CollectionUtil.isEmpty(param.getParamList())) {
                return BigDecimal.ZERO;
            }
            //条件检查
            if ((CollectionUtil.isNotEmpty(condition.getRoomTypeIds()) && !condition.getRoomTypeIds().contains(param.getRoomType()))
                    || (CollectionUtil.isNotEmpty(condition.getExclusionGameIds()) && condition.getExclusionGameIds().contains(param.getGameId()))
                    || (CollectionUtil.isNotEmpty(condition.getGameType()) && condition.getGameType().contains(param.getGameType()))
                    || (CollectionUtil.isNotEmpty(condition.getGameIds()) && !condition.getGameIds().contains(param.getGameId()))) {
                return BigDecimal.ZERO;
            }
            if (conditionTypeId == ConditionType.PLAYER_BET.getId() && param.getRoomType() > 10) {
                return BigDecimal.ZERO;
            }
            BigDecimal progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), BigDecimal.valueOf(param.getParamList().getFirst()));
            return progress.divide(condition.getMinAchievedValue(), RoundingMode.DOWN);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal reduceProgress(Object param, Object condition, long times) {
        if (param instanceof BaseCheckParam checkParam && condition instanceof BaseCheckCondition baseCheckCondition) {
            return countDao.incrBy(checkParam.getFunction(), getCustomId(checkParam.getPlayerId()),
                    baseCheckCondition.getMinAchievedValue().multiply(BigDecimal.valueOf(-times)));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public PlayerEffectiveCondition analysisCondition(List<String> condition) {
        if (condition.isEmpty()) {
            return null;
        }
        ConditionType type = EnumUtil.getBy(ConditionType.class, (t) -> t.getId() == conditionTypeId);
        if (type == null) {
            return null;
        }
        PlayerEffectiveCondition dropCondition = new PlayerEffectiveCondition();
        dropCondition.setMinAchievedValue(new BigDecimal(condition.getFirst()).setScale(2, RoundingMode.DOWN));
        if (condition.size() > 1) {
            List<Integer> list = condition.subList(2, condition.size()).stream().map(Integer::parseInt).toList();
            switch (type) {
                case PLAY_GAME -> dropCondition.setGameIds(list);
                case NOT_PLAY_GAME -> dropCondition.setExclusionGameIds(list);
                case PLAY_GAME_TYPE -> dropCondition.setGameType(list);
                case PLAY_ROOM_TYPE -> dropCondition.setRoomTypeIds(list);
            }
        }
        return dropCondition;
    }

    @Override
    public String getCustomId(long playerId) {
        return getClass().getSimpleName() + conditionTypeId + playerId;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
