package com.jjg.game.core.base.condition.checker;

import com.jjg.game.core.base.condition.CheckParamCategory.EffectiveFlowingParam;
import com.jjg.game.core.base.condition.CheckerParam;
import com.jjg.game.core.base.condition.EConditionComparator;
import com.jjg.game.core.base.condition.IPlayerConditionChecker;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Player;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public static abstract class BaseEffectiveBetChecker extends AbstractProgressConditionChecker {

        @Override
        public boolean check(Player player, List<CheckerParam> comparatorTaget) {
            CheckerParam checkerParam = filterBindParamCheckParam(comparatorTaget);
            if (checkerParam instanceof EffectiveFlowingParam param) {
                String conditionProgressKey = param.getConditionProgressKey();
                Number conditionProgressNumber = dropItemDao.getProgress(conditionProgressKey);
                long curConditionProgress = conditionProgressNumber == null ? 0 : conditionProgressNumber.longValue();
                // 变化的流水
                long flowingValue = param.getFlowingValue();
                // 流水值
                List<Integer> conditionCfg = new ArrayList<>(param.getConditionCfg());
                int targetFlowing = conditionCfg.removeFirst();
                // 总流水值
                long totalFlowing = curConditionProgress + flowingValue;
                if (checkProgress(totalFlowing, targetFlowing, conditionCfg, param)) {
                    return false;
                }
                int times = (int) Math.floor(totalFlowing / (targetFlowing * 1.0));
                // 记录触发次数
                param.setTriggerTimes(times);
                if (param.getNeedUpdateProgress()) {
                    long multi = (long) targetFlowing * times;
                    long newlyProgressFlowing = totalFlowing - multi;
                    dropItemDao.updateProgress(conditionProgressKey, newlyProgressFlowing);
                }
                return true;
            }
            return false;
        }

        protected abstract boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param);
    }

    /**
     * 针对全部游戏的有效流水
     */
    @Component
    public static class PlayerEffectiveBetAllGameChecker extends BaseEffectiveBetChecker {

        @Override
        protected boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param) {
            return totalFlowing < targetFlowing;
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.PLAYER_BETALL.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.PLAYER_BETALL.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.EQ;
        }
    }

    /**
     * 针对普通房间
     */
    @Component
    public static class PlayerEffectiveBetNormalGameChecker extends BaseEffectiveBetChecker {

        @Override
        protected boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param) {
            if (totalFlowing < targetFlowing) {
                return false;
            }
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(param.getGameCfgId());
            if (warehouseCfg == null) {
                return false;
            }
            // 只针对普通游戏
            return warehouseCfg.getRoomType() < GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START;
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
            return EConditionComparator.LT;
        }
    }

    /**
     * 针对普通房间
     */
    @Component
    public static class PlayerEffectiveBetInRangeGameChecker extends BaseEffectiveBetChecker {

        @Override
        protected boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param) {
            if (totalFlowing < targetFlowing) {
                return false;
            }
            // 只针对普通游戏
            return getConditionComparator(param).inRange(conditionCfg, param.getGameCfgId());
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.PLAY_GAME.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.PLAY_GAME.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.IN;
        }
    }

    /**
     * 针对普通房间
     */
    @Component
    public static class PlayerEffectiveBetNotInRangeGameChecker extends BaseEffectiveBetChecker {

        @Override
        protected boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param) {
            if (totalFlowing < targetFlowing) {
                return false;
            }
            // 只针对普通游戏
            return getConditionComparator(param).notinRange(conditionCfg, param.getGameCfgId());
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.NOTPLAY_GAME.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.NOTPLAY_GAME.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.NOT_IN;
        }
    }

    /**
     * 针对普通房间
     */
    @Component
    public static class PlayerEffectiveGameTypeChecker extends BaseEffectiveBetChecker {

        @Override
        protected boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param) {
            if (totalFlowing < targetFlowing) {
                return false;
            }
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(param.getGameCfgId());
            if (warehouseCfg == null) {
                return false;
            }
            // 只针对普通游戏
            return getConditionComparator(param).inRange(conditionCfg, warehouseCfg.getGameType());
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.PLAY_GAMETYPE.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.PLAY_GAMETYPE.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.IN;
        }
    }

    /**
     * 针对普通房间
     */
    @Component
    public static class PlayerEffectiveRoomTypeChecker extends BaseEffectiveBetChecker {

        @Override
        protected boolean checkProgress(
            long totalFlowing, long targetFlowing, List<Integer> conditionCfg, EffectiveFlowingParam param) {
            if (totalFlowing < targetFlowing) {
                return false;
            }
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(param.getGameCfgId());
            if (warehouseCfg == null) {
                return false;
            }
            // 只针对普通游戏
            return getConditionComparator(param).inRange(conditionCfg, warehouseCfg.getRoomType());
        }

        @Override
        public String bindConditionCheckType() {
            return EGameEventType.PLAY_ROOMTYPE.name();
        }

        @Override
        public List<String> bindConditionCheckParam() {
            return EGameEventType.PLAY_ROOMTYPE.getBindProperties();
        }

        @Override
        public EConditionComparator defaultConditionComparator() {
            return EConditionComparator.IN;
        }
    }
}
