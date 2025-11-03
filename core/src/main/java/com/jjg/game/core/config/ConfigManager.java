package com.jjg.game.core.config;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.ClassUtils;
import com.jjg.game.common.utils.ObjectMapperUtil;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 配置管理器
 * <p>
 * <span style="color:red">如果需要全部加载配置则需要调用{@link ConfigManager#loadAll} </span>
 * <p>
 * <span style="color:red">否则需要使用{@link ConfigManager#addLoadConfig(Supplier)}指定加载</span>
 */
@Component
@SuppressWarnings("unchecked")
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

    /**
     * 配置变化监听器 k=配置文件class v=监听器列表
     */
    private final ConcurrentHashMap<Class<?>, List<ConfigUpdateHandler<? extends AbstractExcelConfig>>> updateConfigListenerMap = new ConcurrentHashMap<>();

    /**
     * 默认加载所有配置
     */
    private boolean loadAll = false;

    /**
     * 存储需要加载配置类的集合
     */
    private final Set<Class<?>> loadConfigSet = new HashSet<>();

    /**
     * 用于管理和执行任务的线程池。采用每个任务分配一个虚拟线程的执行器，能够高效管理任务执行。
     * 基于虚拟线程的模型，减少线程阻塞问题，提高并发能力。
     * 此执行器在配置加载和更新过程中，帮助提高任务执行的效率。
     */
    private final ExecutorService executor;

    /**
     * json转换对象
     */
    private final ObjectMapper objectMapper;

    public ConfigManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.objectMapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        init();
    }

    /**
     * 加载所有配置
     */
    public void loadAll() {
        this.loadAll = true;
        loadAllConfigsFromRedis();
    }

    /**
     * 指定加载配置文件,如果没有{@link ConfigManager#loadAll()}则需要调用本方法添加加载类
     *
     * @param supplier
     */
    public void addLoadConfig(Supplier<List<Class<? extends AbstractExcelConfig>>> supplier) {
        loadConfigSet.addAll(supplier.get());
        loadAllConfigsFromRedis();
    }

    /**
     * 初始化
     */
    public void init() {
        ClassUtils.getAllClassByAnnotation(CoreConst.Common.BASE_PROJECT_PACKAGE_PATH, ExcelConfig.class)
                .forEach(configClazz -> {
                    ExcelConfig excelConfig = configClazz.getAnnotation(ExcelConfig.class);
                    configClazzMap.put(excelConfig.name(), configClazz);
                    configNameMap.put(configClazz, excelConfig.name());
                });
    }

    /**
     * 从Redis加载所有配置到本地缓存
     */
    private void loadAllConfigsFromRedis() {
        Map<String, Map<Integer, AbstractExcelConfig>> map = getRedisConfigMap();
        if (loadAll) {
            localCache.putAll(map);
        } else {
            if (!loadConfigSet.isEmpty()) {
                loadConfigSet.forEach(clazz -> {
                    String name = getConfigName(clazz);
                    if (name != null) {
                        Map<Integer, AbstractExcelConfig> excelConfigMap = map.get(name);
                        if (excelConfigMap != null) {
                            localCache.put(name, excelConfigMap);
                        }
                    }
                });
            }
        }
        log.info("配置管理器初始化完成，加载了 {} 个配置类型", localCache.size());
    }

    /**
     * 获取存储在redis中的配置信息
     * @return redis中的配置信息
     */
    private Map<String, Map<Integer, AbstractExcelConfig>> getRedisConfigMap() {
        RMap<String, String> redisMap = redissonClient.getMap(CONFIG_MAP_KEY);
        Map<String, Map<Integer, AbstractExcelConfig>> map = new HashMap<>();
        for (Map.Entry<String, String> entry : redisMap.entrySet()) {
            try {
                map.put(entry.getKey(), objectMapper.readValue(entry.getValue(), new TypeReference<>() {
                }));
            } catch (Exception e) {
                log.error("load config from redis map error!", e);
            }
        }
        return map;
    }

    /**
     * 从Redis加载所有配置到本地缓存
     */
    public void reLoadAllConfigsFromRedis(String name) {
        Map<String, Map<Integer, AbstractExcelConfig>> redisMap = getRedisConfigMap();
        Map<Integer, AbstractExcelConfig> excelConfigMap = redisMap.get(name);
        log.info("收到更新配置消息!name={}", name);
        if (excelConfigMap != null) {
            List<AbstractExcelConfig> configList = excelConfigMap.values().stream().toList();
            replaceConfig(name, configList);
        }
    }

    /**
     * 刷新所有配置的本地缓存
     */
    public void refreshAllConfigsFromRedis() {
        try {
            // 重新从Redis加载所有配置到本地缓存
            loadAllConfigsFromRedis();
            log.info("已刷新所有配置的本地缓存");
        } catch (Exception e) {
            log.error("刷新所有配置失败: {}", e.getMessage(), e);
        }
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
     * @param <T>   配置类型
     * @return 返回指定类型的配置对象，若未找到则返回null
     */
    public <T extends AbstractExcelConfig> T getConfig(Class<T> clazz, int id) {
        String name = getConfigName(clazz);
        if (name == null) {
            return null;
        }
        AbstractExcelConfig config = getConfig(name, id);
        return config != null ? (T) config : null;
    }

    /**
     * 获取配置列表
     *
     * @param name 表名
     * @param <T>  配置类型
     * @return 返回指定类型的配置列表
     */
    public <T extends AbstractExcelConfig> List<T> getConfigs(String name) {
        Map<Integer, AbstractExcelConfig> excelConfigMap = localCache.get(name);
        if (excelConfigMap == null) {
            return Collections.emptyList();
        }
        return excelConfigMap.values().stream().map(c -> (T) c).toList();
    }

    /**
     * 获取配置列表
     *
     * @param clazz 配置类class
     * @param <T>   配置类型
     * @return 返回指定类型的配置列表
     */
    public <T extends AbstractExcelConfig> List<T> getConfigs(Class<T> clazz) {
        String name = getConfigName(clazz);
        if (name == null) {
            return Collections.emptyList();
        }
        return getConfigs(name);
    }

    /**
     * 根据配置类获取配置名称
     *
     * @param clazz 配置类
     * @return 配置名称，如果未找到则返回null
     */
    private String getConfigName(Class<?> clazz) {
        return configNameMap.get(clazz);
    }

    /**
     * 根据配置名称获取配置类
     *
     * @param name 配置名称
     * @return 配置类，如果未找到则返回null
     */
    private Class<?> getConfigClass(String name) {
        return configClazzMap.get(name);
    }

    /**
     * 覆盖配置信息
     */
    public void replaceConfigStrList(String name, List<String> configStrList) {
        Class<?> clazz = getConfigClass(name);
        List<AbstractExcelConfig> list = configStrList.stream().map(str -> (AbstractExcelConfig) JSON.parseObject(str, clazz)).toList();
        replaceConfig(name, list);
    }

    /**
     * 检测配置是否处理
     *
     * @param name 表名
     * @return true 不处理
     */
    public boolean checkConfig(String name) {
        Class<?> configClas = getConfigClass(name);
        return checkConfig(configClas);
    }

    /**
     * 检测配置是否处理
     *
     * @return true 不处理
     */
    public boolean checkConfig(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        if (!loadAll) {
            //不处理
            return !loadConfigSet.contains(clazz);
        }
        return false;
    }

    /**
     * 覆盖配置信息
     *
     * @param name    配置表名称
     * @param configs 要更新的配置列表
     *                <p>
     *                集群环境下的数据一致性保证：
     *                1. 使用分布式锁确保同一时间只有一个节点在更新配置
     *                2. 先更新Redis数据，再更新本地缓存，确保数据持久化
     *                3. 异常处理确保锁的正确释放
     */
    public void replaceConfig(String name, List<AbstractExcelConfig> configs) {
        //检测是否需要处理
        if (checkConfig(name)) {
            return;
        }
        RMap<String, String> configMap = redissonClient.getMap(CONFIG_MAP_KEY);
        try {
            configMap.getLock(name).lock();
            boolean isReplace = false;
            try {
                String jsonString = configMap.get(name);
                Map<Integer, AbstractExcelConfig> excelConfigMap = objectMapper.readValue(jsonString, new TypeReference<>() {
                });
//                log.debug("beforeMap = {}",JSON.toJSONString(excelConfigMap));
                configs.forEach(config -> {
                    // 先更新Redis数据
                    excelConfigMap.put(config.getId(), config);
                });
//                log.debug("afterMap = {}",JSON.toJSONString(excelConfigMap));
                configMap.put(name, objectMapper.writeValueAsString(excelConfigMap));
                isReplace = true;
                log.info("批量覆盖[{}]的配置[{}]条! ", name, configs.size());
            } catch (Exception e) {
                log.error("replaceConfig error! name:{}", name, e);
            } finally {
                configMap.getLock(name).unlock();
            }
            if (!isReplace) {
                return;
            }
            //更新本地缓存
            configs.forEach(config -> {
                Map<Integer, AbstractExcelConfig> excelConfigMap = localCache.computeIfAbsent(name, k -> new ConcurrentHashMap<>());
                AbstractExcelConfig oldConfig = excelConfigMap.get(config.getId());
                if (oldConfig != null) {
                    //配置更新
                    if (!oldConfig.computeMd5().equals(config.computeMd5())) {
                        excelConfigMap.put(config.getId(), config);
                        notifyUpdateConfig(name, ConfigChangeState.UPDATE, config);
                    }
                }
                //新增
                else {
                    excelConfigMap.put(config.getId(), config);
                    notifyUpdateConfig(name, ConfigChangeState.ADD, config);
                }
            });
        } catch (Exception e) {
            log.error("更新配置[{}]失败: {}", name, e.getMessage(), e);
            throw new RuntimeException("配置更新失败", e);
        }
    }

    /**
     * 删除配置信息
     *
     * @param name 配置表名称
     * @param ids  要删除的配置ID列表
     *             <p>
     *             集群环境下的删除操作保证：
     *             1. 使用分布式锁确保删除操作的原子性
     *             2. 同时从Redis和本地缓存中删除数据
     *             3. 防止并发删除导致的数据不一致问题
     */
    public void deleteConfig(String name, List<Integer> ids) {
        //检测是否需要处理
        if (checkConfig(name)) {
            return;
        }
        RMap<String, Map<Integer, AbstractExcelConfig>> configMap = redissonClient.getMap(CONFIG_MAP_KEY);
        try {
            // 使用分布式锁确保Redis和本地缓存的一致性
            // 防止多个节点同时删除同一配置导致的数据不一致
            configMap.getLock(name).lock();
            try {
                Map<Integer, AbstractExcelConfig> excelConfigMap = configMap.get(name);
                ids.forEach(id -> {
                    // 从Redis中删除配置数据
                    if (excelConfigMap != null) {
                        excelConfigMap.remove(id);
                    }
                });
                configMap.put(name, excelConfigMap);
                log.info("删除[{}]的配置ids:[{}]的配置!", name, ids);
            } finally {
                // 确保锁被正确释放，防止死锁
                configMap.getLock(name).unlock();
            }
            Map<Integer, AbstractExcelConfig> excelConfigs = localCache.get(name);
            ids.forEach(id -> {
                // 从本地缓存中删除配置数据
                if (excelConfigs != null) {
                    AbstractExcelConfig config = excelConfigs.remove(id);
                    notifyUpdateConfig(name, ConfigChangeState.DELETE, config);
                }
            });
        } catch (Exception e) {
            log.error("删除配置[{}]失败: {}", name, e.getMessage(), e);
            throw new RuntimeException("配置删除失败", e);
        }
    }

    /**
     * GM通知从redis中同步配置到本地缓存中
     *
     * @param name excel表名
     */
    public void syncConfigFromRedis(String name) {
        //检测是否需要处理
        if (checkConfig(name)) {
            return;
        }
        Class<?> configClass = getConfigClass(name);
        if (configClass == null) {
            return;
        }
        RMap<String, Map<Integer, AbstractExcelConfig>> configMap = redissonClient.getMap(CONFIG_MAP_KEY);
        Map<Integer, AbstractExcelConfig> excelConfigMap = configMap.get(name);
        if (excelConfigMap == null) {
            return;
        }
        List<AbstractExcelConfig> configs = excelConfigMap.values().stream().toList();
        if (configs.isEmpty()) {
            return;
        }
        try {
            Map<Integer, AbstractExcelConfig> localConfigMap = localCache.get(name);
            Map<Integer, AbstractExcelConfig> oldLocal = null;
            if (localConfigMap == null) {
                localConfigMap = new ConcurrentHashMap<>();
            } else {
                oldLocal = new ConcurrentHashMap<>(localConfigMap);
                localConfigMap.clear();
            }
            AbstractExcelConfig oldConfig;
            for (AbstractExcelConfig config : configs) {
                //更新本地缓存
                localConfigMap.put(config.getId(), config);
                if (oldLocal != null) {
                    oldConfig = oldLocal.get(config.getId());
                    //新增配置
                    if (oldConfig == null) {
                        notifyUpdateConfig(name, ConfigChangeState.ADD, config);
                    }
                    //更新
                    else {
                        if (!oldConfig.computeMd5().equals(config.computeMd5())) {
                            notifyUpdateConfig(name, ConfigChangeState.UPDATE, config);
                        }
                    }
                }
                //新增
                else {
                    notifyUpdateConfig(name, ConfigChangeState.ADD, config);
                }
            }
            //检测配置是否删除
            if (oldLocal != null) {
                for (Map.Entry<Integer, AbstractExcelConfig> entry : oldLocal.entrySet()) {
                    int id = entry.getKey();
                    AbstractExcelConfig config = entry.getValue();
                    if (!localConfigMap.containsKey(id)) {
                        notifyUpdateConfig(name, ConfigChangeState.DELETE, config);
                    }
                }
            }
        } catch (Exception e) {
            log.error("更新配置[{}]失败: {}", name, e.getMessage(), e);
            throw new RuntimeException("配置更新失败", e);
        }
    }


    /**
     * 同步配置
     *
     * @param name    excel表名
     * @param configs 同步的配置信息
     */
    public void syncStrConfigs(String name, List<String> configs) {
        //检测是否需要处理
        if (checkConfig(name)) {
            return;
        }
        Class<?> configClass = getConfigClass(name);
        if (configClass == null) {
            return;
        }
        List<AbstractExcelConfig> configList = configs.stream().map(str -> (AbstractExcelConfig) JSON.parseObject(str, configClass)).toList();
        if (configList.isEmpty()) {
            return;
        }
        log.debug("同步配置 name = {},configs = {}", name, JSON.toJSONString(configList));
        syncConfigs(name, configList);
    }

    /**
     * 同步配置
     *
     * @param name    excel表名
     * @param configs 同步的配置信息
     */
    public void syncConfigs(String name, List<AbstractExcelConfig> configs) {
        //检测是否需要处理
        if (checkConfig(name)) {
            return;
        }

        Map<Integer, AbstractExcelConfig> tmpExcelConfigMap = new ConcurrentHashMap<>();
        for (AbstractExcelConfig config : configs) {
            tmpExcelConfigMap.put(config.getId(), config);
        }

        RMap<String, Map<Integer, AbstractExcelConfig>> configMap = redissonClient.getMap(CONFIG_MAP_KEY);
        try {
            configMap.getLock(name).lock();
            try {
                // 先更新Redis数据
//                Map<Integer, AbstractExcelConfig> excelConfigMap = configMap.get(name);
//                if (excelConfigMap == null) {
//                    excelConfigMap = new ConcurrentHashMap<>();
//                } else {
//                    excelConfigMap.clear();
//                }
//                for (AbstractExcelConfig config : configs) {
//                    excelConfigMap.put(config.getId(), config);
//                }
                configMap.put(name, tmpExcelConfigMap);
                log.info("同步[{}]的配置[{}]条!  map = {}", name, configs.size(), JSON.toJSONString(tmpExcelConfigMap));
            } finally {
                configMap.getLock(name).unlock();
            }
            Map<Integer, AbstractExcelConfig> localConfigMap = localCache.get(name);
            Map<Integer, AbstractExcelConfig> oldLocal = null;
            if (localConfigMap == null) {
                localConfigMap = new ConcurrentHashMap<>();
            } else {
                oldLocal = new ConcurrentHashMap<>(localConfigMap);
                localConfigMap.clear();
            }
            AbstractExcelConfig oldConfig;
            for (AbstractExcelConfig config : configs) {
                //更新本地缓存
                localConfigMap.put(config.getId(), config);
                if (oldLocal != null) {
                    oldConfig = oldLocal.get(config.getId());
                    //新增配置
                    if (oldConfig == null) {
                        notifyUpdateConfig(name, ConfigChangeState.ADD, config);
                    }
                    //更新
                    else {
                        if (!oldConfig.computeMd5().equals(config.computeMd5())) {
                            notifyUpdateConfig(name, ConfigChangeState.UPDATE, config);
                        }
                    }
                }
                //新增
                else {
                    notifyUpdateConfig(name, ConfigChangeState.ADD, config);
                }
            }
            //检测配置是否删除
            if (oldLocal != null) {
                for (Map.Entry<Integer, AbstractExcelConfig> entry : oldLocal.entrySet()) {
                    Integer key = entry.getKey();
                    AbstractExcelConfig config = entry.getValue();
                    if (!tmpExcelConfigMap.containsKey(key)) {
                        notifyUpdateConfig(name, ConfigChangeState.DELETE, config);
                    }
                }
            }
        } catch (Exception e) {
            log.error("更新配置[{}]失败: {}", name, e.getMessage(), e);
            throw new RuntimeException("配置更新失败", e);
        }
    }

    /**
     * 添加配置变化监听
     *
     * @param clazz   配置类
     * @param handler 配置更新处理器
     * @param <T>     配置类型
     */
    public <T extends AbstractExcelConfig> void addUpdateConfigListener(Class<T> clazz, ConfigUpdateHandler<T> handler) {
        updateConfigListenerMap.computeIfAbsent(clazz, k -> Collections.synchronizedList(new ArrayList<>())).add(handler);
    }

    /**
     * 通知配置更新的方法
     *
     * @param name      配置表的名称
     * @param state     配置的变化状态
     * @param newConfig 新的配置信息，当配置状态为删除时，此参数代表被删除的旧配置，其它状态下为新的配置信息。
     */
    public <T extends AbstractExcelConfig> void notifyUpdateConfig(String name, ConfigChangeState state, AbstractExcelConfig newConfig) {
        Class<?> configClass = getConfigClass(name);
        if (configClass == null) {
            return;
        }
        List<ConfigUpdateHandler<? extends AbstractExcelConfig>> configListenerList = updateConfigListenerMap.get(configClass);
        if (configListenerList != null && !configListenerList.isEmpty()) {
            configListenerList.forEach(handler -> executor.submit(() -> {
                try {
                    ((ConfigUpdateHandler<T>) handler).accept(name, state, (T) newConfig);
                } catch (Exception e) {
                    log.error("配置[{}]state[{}]通知更新失败!", name, state, e);
                }
            }));
        }
    }

}
