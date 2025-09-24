package com.jjg.game.core.config;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.ClassUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器
 * 优化方案：本地缓存 + Redis持久化
 * 适用于：读取频繁，修改不频繁的场景
 */
@Component
public class ConfigManager {

    private final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    public static final String CONFIG_MAP_KEY = "excelConfigMap:";

    private final RedissonClient redissonClient;

    /**
     * 配置信息缓存
     */
    private final ConcurrentHashMap<String, Map<Integer, AbstractExcelConfig>> localCache = new ConcurrentHashMap<>();

    /**
     * 配置excel名字到类映射
     */
    private final Map<String, Class<?>> configClazzMap = new HashMap<>();

    /**
     * 类到配置excel名字映射
     */
    private final Map<Class<?>, String> configNameMap = new HashMap<>();

    public ConfigManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 初始化：从Redis加载所有配置到本地缓存
     */
    public void init() {
        ClassUtils.getAllClassByAnnotation(CoreConst.Common.BASE_PROJECT_PACKAGE_PATH, ExcelConfig.class)
                .forEach(configClazz -> {
                    ExcelConfig excelConfig = configClazz.getAnnotation(ExcelConfig.class);
                    configClazzMap.put(excelConfig.name(), configClazz);
                    configNameMap.put(configClazz, excelConfig.name());
                });
        // 从Redis加载所有配置到本地缓存
        loadAllConfigsFromRedis();
        log.info("配置管理器初始化完成，加载了 {} 个配置类型", localCache.size());
    }

    /**
     * 从Redis加载所有配置到本地缓存
     */
    private void loadAllConfigsFromRedis() {
        RMap<String, Map<Integer, AbstractExcelConfig>> redisMap = redissonClient.getMap(CONFIG_MAP_KEY);
        localCache.putAll(redisMap);
    }

    /**
     * 获取指定名称和ID的Excel配置。
     *
     * @param name 配置表的名称。
     * @param id   配置的唯一标识符。
     * @return 返回对应的Excel配置对象，若未找到则返回null。
     */
    public AbstractExcelConfig getConfig(String name, int id) {
        Map<Integer, AbstractExcelConfig> excelConfigMap = localCache.get(name);
        if (excelConfigMap == null) {
            return null;
        }
        return excelConfigMap.get(id);
    }

    /**
     * 获取配置
     *
     * @param clazz 配置类class
     * @param id    配置id
     */
    public AbstractExcelConfig getConfig(Class<?> clazz, int id) {
        String name = configNameMap.get(clazz);
        if (name == null) {
            return null;
        }
        return getConfig(name, id);
    }

    /**
     * 获取配置列表
     *
     * @param name 表名
     */
    public List<AbstractExcelConfig> getConfigs(String name) {
        Map<Integer, AbstractExcelConfig> excelConfigMap = localCache.get(name);
        if (excelConfigMap == null) {
            return Collections.emptyList();
        }
        return excelConfigMap.values().stream().toList();
    }

    /**
     * 获取配置列表
     */
    public List<AbstractExcelConfig> getConfigs(Class<?> clazz) {
        String name = configNameMap.get(clazz);
        if (name == null) {
            return Collections.emptyList();
        }
        return getConfigs(name);
    }

    /**
     * 覆盖配置信息
     */
    public void replaceConfigStrList(String name, List<String> configStrList) {
        Class<?> clazz = configClazzMap.get(name);
        List<AbstractExcelConfig> list = configStrList.stream().map(str -> (AbstractExcelConfig) JSON.parseObject(str, clazz)).toList();
        replaceConfig(name, list);
    }

    /**
     * 覆盖配置信息
     */
    public void replaceConfig(String name, List<AbstractExcelConfig> configs) {
        RMap<String, Map<Integer, AbstractExcelConfig>> configMap = redissonClient.getMap(CONFIG_MAP_KEY);
        configs.forEach(config -> {
            //先更新redis数据
            configMap.computeIfAbsent(name, k -> new ConcurrentHashMap<>()).put(config.getId(), config);
            //更新本地
            localCache.computeIfAbsent(name, k -> new ConcurrentHashMap<>()).put(config.getId(), config);
        });
        log.info("批量覆盖[{}]的配置[{}]条!", name, configs.size());
    }

    /**
     * 删除配置信息
     */
    public void deleteConfig(String name, List<Integer> ids) {
        RMap<String, Map<Integer, AbstractExcelConfig>> configMap = redissonClient.getMap(CONFIG_MAP_KEY);
        Map<Integer, AbstractExcelConfig> excelConfigMap = configMap.get(name);
        Map<Integer, AbstractExcelConfig> excelConfigs = localCache.get(name);
        ids.forEach(id -> {
            if (excelConfigMap != null) {
                excelConfigMap.remove(id);
            }
            if (excelConfigs != null) {
                excelConfigs.remove(id);
            }
        });
        log.info("删除[{}]的配置ids:[{}]的配置!", name, ids);
    }

}
