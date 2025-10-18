package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.PlayerGameWinMoneyCondition;
import com.jjg.game.core.base.condition.check.record.PlayerSampleParam;

import java.util.List;

/**
 * 10003_游戏ID(0 = 任意游戏)（不区分倍场）_大于等于总押注条件_大于等于目标获胜金额_货币ID(金币或钻石)
 *
 * @author lm
 * @date 2025/10/16 17:52
 */
public class PlayerGameWinMoneyCheck extends BaseCheck {

    @Override
    public boolean check(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerGameWinMoneyCondition condition) {

            //总押注 总获胜金额 货币id
            List<Long> paramList = param.getParamList();
            if (paramList == null || paramList.size() != 3) {
                return false;
            }
            //道具id检查
            if (paramList.getLast().intValue() != condition.getItemId()) {
                return false;
            }
            if (CollectionUtil.isEmpty(condition.getIds()) || condition.getIds().contains(param.getId())) {
                Long first = paramList.getFirst();
                return first >= condition.getNeedBet() && paramList.get(1) >= condition.getMinAchievedValue();
            }
        }
        return false;
    }

    @Override
    public PlayerGameWinMoneyCondition analysisCondition(List<Integer> condition) {
        if (condition.size() < 4) {
            return null;
        }
        PlayerGameWinMoneyCondition sampleCondition = new PlayerGameWinMoneyCondition();
        if (condition.getFirst() > 0) {
            sampleCondition.setIds(List.of(condition.getFirst()));
        }
        sampleCondition.setNeedBet(condition.get(1));
        sampleCondition.setMinAchievedValue(condition.get(2));
        sampleCondition.setItemId(condition.get(3));
        return sampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}

