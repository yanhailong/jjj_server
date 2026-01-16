package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:34
 */
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConditionRegistry {

    private final Map<String, ConditionHandler<?>> handlers = new HashMap<>();

    public ConditionRegistry(List<ConditionHandler<?>> list) {
        for (ConditionHandler<?> h : list) {
            handlers.put(h.type(), h);
        }
    }

    @SuppressWarnings("unchecked")
    public <C> ConditionHandler<C> get(String type) {
        return (ConditionHandler<C>) handlers.get(type);
    }
}
