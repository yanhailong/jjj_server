package com.jjg.game.core.service;

import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.dao.CountDao;
import org.springframework.stereotype.Service;

/**
 * 接取型数值条件的通用 Redis 进度存储。
 * <p>
 * 条件只定义如何从事实事件得到进度变化；具体功能必须显式传入接取实例作用域，避免同一条件在任务、
 * 赛季等功能间共享进度。最终 Redis key 为
 * {@code count:stat:<conditionId>:<functionType>:<instanceId>}，hash field 为 playerId。
 */
@Service
public class AcceptedConditionProgressService {
    private final CountDao countDao;

    public AcceptedConditionProgressService(CountDao countDao) {
        this.countDao = countDao;
    }

    public void begin(long playerId, PreparedCondition condition, Scope scope) {
        clear(playerId, condition.spec().id(), scope);
    }

    public ProgressResult advance(long playerId, PreparedCondition condition, Scope scope,
                                  ConditionEvent event) {
        ConditionUpdate update = condition.evaluate(event);
        String featureId = featureId(condition.spec().id(), scope);
        String field = Long.toString(playerId);
        if (!update.matched() || update.value() <= 0) {
            return new ProgressResult(false, countDao.getCountHashLong(featureId, field));
        }
        long progress = switch (update.mode()) {
            case ADD -> countDao.incrHashLong(playerId, featureId, field, update.value());
            case MAX -> countDao.maxHashLong(playerId, featureId, field, update.value());
            case SET -> {
                countDao.setHashLong(playerId, featureId, field, update.value());
                yield update.value();
            }
        };
        return new ProgressResult(true, progress);
    }

    public long progress(long playerId, PreparedCondition condition, Scope scope) {
        return countDao.getCountHashLong(featureId(condition.spec().id(), scope), Long.toString(playerId));
    }

    public void clear(long playerId, PreparedCondition condition, Scope scope) {
        clear(playerId, condition.spec().id(), scope);
    }

    public void clear(long playerId, int conditionId, Scope scope) {
        countDao.resetHashLong(playerId, featureId(conditionId, scope), Long.toString(playerId));
    }

    private static String featureId(int conditionId, Scope scope) {
        if (conditionId <= 0) {
            throw new IllegalArgumentException("conditionId must be positive");
        }
        return CountDao.CountType.PLAYER_STAT.getParam().formatted(
                conditionId + ":" + scope.functionType() + ":" + scope.instanceId());
    }

    public record Scope(String functionType, String instanceId) {
        public Scope {
            validatePart(functionType, "functionType");
            validatePart(instanceId, "instanceId");
        }

        public static Scope of(String functionType, long instanceId) {
            return new Scope(functionType, Long.toString(instanceId));
        }

        private static void validatePart(String value, String name) {
            if (value == null || value.isBlank() || value.indexOf(':') >= 0) {
                throw new IllegalArgumentException(name + " must be non-blank and cannot contain ':'");
            }
        }
    }

    public record ProgressResult(boolean matched, long progress) {
    }
}
