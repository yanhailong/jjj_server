package com.jjg.game.core.config;

/**
 * 配置更新处理器
 *
 * @param <T> 配置类型
 */
@FunctionalInterface
public interface ConfigUpdateHandler<T extends AbstractExcelConfig> {

    /**
     * 处理配置更新
     *
     * @param name   excel表名
     * @param state  变化状态
     * @param config 变化的配置,只有删除是老配置 其他都是新配置
     */
    void accept(String name, ConfigChangeState state, T config);

}
