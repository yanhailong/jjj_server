package com.jjg.game.core.base.condition.numeric;

import java.util.ArrayList;
import java.util.List;

/**
 * condition 表的一条运行时配置。
 * <p>
 * {@code id} 永远取 condition 表的 id 列；{@code parameters} 只保存 id 后面的参数。
 * 文本配置同时接受下划线和星号分隔，例如 {@code 12201_100100_100} 与
 * {@code 10003_100100*1*100*1001}。
 */
public record ConditionSpec(int id, List<Long> parameters) {

    public ConditionSpec {
        if (id <= 0) {
            throw new IllegalArgumentException("condition id must be positive: " + id);
        }
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /**
     * 从 Excel 生成的数值列表解析，首位是条件 id。
     */
    public static ConditionSpec from(List<Long> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("condition config must contain an id");
        }
        long rawId = values.getFirst();
        if (rawId <= 0 || rawId > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid condition id: " + rawId);
        }
        return new ConditionSpec((int) rawId, values.subList(1, values.size()));
    }

    /**
     * 将旧 {@code ConditionHandler} 已拆分的文本参数接入统一数值配置，不在 handler 中重复定义参数顺序。
     */
    public static ConditionSpec from(int id, List<String> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("condition parameters must not be null");
        }
        List<Long> values = new ArrayList<>(parameters.size());
        for (String parameter : parameters) {
            if (parameter == null || parameter.isBlank()) {
                throw new IllegalArgumentException("condition " + id + " contains an empty parameter");
            }
            try {
                values.add(Long.parseLong(parameter.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("condition " + id
                        + " parameter is not a number: " + parameter, e);
            }
        }
        return new ConditionSpec(id, values);
    }

    /**
     * 从后台文本配置解析。连续分隔符和非数字参数会直接报错，避免错误配置静默上线。
     */
    public static ConditionSpec parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("condition config must not be blank");
        }
        String[] tokens = value.trim().split("[_*]", -1);
        List<Long> values = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (token.isBlank()) {
                throw new IllegalArgumentException("condition config contains an empty parameter: " + value);
            }
            try {
                values.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("condition parameter is not a number: " + token, e);
            }
        }
        return from(values);
    }

    public long parameter(int index) {
        return parameters.get(index);
    }

    public int intParameter(int index) {
        return Math.toIntExact(parameter(index));
    }
}
