package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:47
 */
import java.util.concurrent.ConcurrentHashMap;

public class ConditionTreeCache {

    private final ConcurrentHashMap<String, ConditionNode> cache = new ConcurrentHashMap<>();
    private final ConditionParser parser;

    public ConditionTreeCache(ConditionParser parser) {
        this.parser = parser;
    }

    public ConditionNode getOrParse(String expr) {
        return cache.computeIfAbsent(expr, parser::parse);
    }

    /** 配置热更时调用 */
    public void clear() {
        cache.clear();
    }

    public void remove(String expr) {
        cache.remove(expr);
    }
}
