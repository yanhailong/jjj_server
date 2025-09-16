package com.jjg.game.core.base.condition.checker;

import com.jjg.game.core.base.condition.CheckerParam;
import com.jjg.game.core.base.condition.EConditionComparator;
import com.jjg.game.core.base.condition.IPlayerConditionChecker;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.data.Player;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 玩家相关检查
 *
 * @author 2CL
 */
@Component
public class PlayerCheckerCategory {

    @Component
    public static class PlayerLevelChecker implements IPlayerConditionChecker {

        @Override
        public boolean check(Player player, List<CheckerParam> comparatorTaget) {
            CheckerParam checkerParam = filterBindParamCheckParam(comparatorTaget);
            int targetLevel;
            if (checkerParam.getTargetParam() instanceof List<?> list) {
                targetLevel = (int) list.getFirst();
            } else {
                targetLevel = (int) checkerParam.getTargetParam();
            }
            return getConditionComparator(checkerParam).intComparate(player.getLevel(), targetLevel);
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.PLAYER_LEVEL.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.PLAYER_LEVEL.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.GTE;
        }
    }

    @Component
    public static class PlayerEffectiveBetAllGameChecker extends AbstractProgressConditionChecker {

        @Override
        public boolean check(Player player, List<CheckerParam> comparatorTaget) {
            CheckerParam checkerParam = filterBindParamCheckParam(comparatorTaget);

            return false;
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.PLAYER_BET.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.PLAYER_BET.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.GTE;
        }
    }
}
