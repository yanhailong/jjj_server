package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:35
 */

import com.jjg.game.core.base.condition.conditionnode.AndNode;
import com.jjg.game.core.base.condition.conditionnode.AtomicNode;
import com.jjg.game.core.base.condition.conditionnode.NotNode;
import com.jjg.game.core.base.condition.conditionnode.OrNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConditionParser {

    private final ConditionRegistry registry;

    public ConditionParser(ConditionRegistry registry) {
        this.registry = registry;
    }

    public ConditionNode parse(String expr) {
        expr = expr.trim();

        if (expr.startsWith("AND(")) {
            return new AndNode(parseChildren(expr));
        }
        if (expr.startsWith("OR(")) {
            return new OrNode(parseChildren(expr));
        }
        if (expr.startsWith("NOT(")) {
            return new NotNode(parse(inner(expr)));
        }
        return parseAtomic(expr);
    }

    private List<ConditionNode> parseChildren(String expr) {
        String inner = inner(expr);
        List<String> parts = split(inner);

        List<ConditionNode> nodes = new ArrayList<>();
        for (String p : parts) {
            nodes.add(parse(p));
        }
        return nodes;
    }

    private ConditionNode parseAtomic(String expr) {
        int idx = expr.indexOf('(');
        String type = expr.substring(0, idx);
        String args = inner(expr);

        List<String> params = split(args);
        ConditionHandler<?> handler = registry.get(type);

        if (handler == null) {
            throw new IllegalArgumentException("Unknown condition type: " + type);
        }

        Object cfg = handler.parse(params);
        return new AtomicNode(handler, cfg);
    }

    private String inner(String s) {
        return s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'));
    }

    private List<String> split(String s) {
        List<String> res = new ArrayList<>();
        int level = 0;
        StringBuilder buf = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (c == '(') level++;
            if (c == ')') level--;
            if (c == ',' && level == 0) {
                res.add(buf.toString().trim());
                buf.setLength(0);
            } else {
                buf.append(c);
            }
        }
        if (!buf.isEmpty()) res.add(buf.toString().trim());
        return res;
    }
}
