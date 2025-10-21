package com.jjg.game.core.manager;

import cn.hutool.core.lang.Snowflake;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.core.constant.AwardCodeType;
import com.jjg.game.core.dao.AwardCodeDao;
import com.jjg.game.core.data.AwardCode;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.sqids.Sqids;

import java.util.List;

/**
 * 领奖码生成器
 * 基于雪花算法生成唯一ID，并使用Sqids进行编码混淆
 */
@Component
public class AwardCodeManager {
    private static final Logger log = LoggerFactory.getLogger(AwardCodeManager.class);

    /**
     * Sqids编码使用的字符表
     */
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Redis中存储Worker ID映射的Key
     */
    private static final String WORKER_ID_MAP_KEY = "snowflake:workerId:map";

    /**
     * Redis中存储Worker ID计数器的Key
     */
    private static final String WORKER_ID_COUNTER_KEY = "snowflake:workerId:counter";

    /**
     * Redis中Worker ID分配锁的Key
     */
    private static final String WORKER_ID_LOCK_KEY = "snowflake:workerId:lock";

    /**
     * Worker ID的最大值（雪花算法限制：0~31）
     */
    private static final long MAX_WORKER_ID = 31;

    /**
     * 数据中心ID的最大值（雪花算法限制：0~31）
     */
    private static final long MAX_DATACENTER_ID = 31;

    /**
     * 领奖码的最小长度
     */
    private static final int MIN_CODE_LENGTH = 10;

    /**
     * 雪花ID编解码器
     */
    private final Sqids sqids;

    /**
     * 雪花ID生成器实例
     */
    private final Snowflake snowflake;

    /**
     * 领奖码数据访问对象
     */
    private final AwardCodeDao awardCodeDao;

    /**
     * Redis客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 集群系统
     */
    private final ClusterSystem clusterSystem;

    /**
     * 构造函数，初始化领奖码管理器
     *
     * @param awardCodeDao   领奖码DAO
     * @param redissonClient Redis客户端
     * @param clusterSystem  集群系统
     */
    public AwardCodeManager(AwardCodeDao awardCodeDao, RedissonClient redissonClient, ClusterSystem clusterSystem) {
        this.awardCodeDao = awardCodeDao;
        this.redissonClient = redissonClient;
        this.clusterSystem = clusterSystem;
        this.sqids = createSqidsEncoder();
        this.snowflake = initSnowflake();
        log.info("领奖码管理器初始化成功");
    }

    /**
     * 创建Sqids编码器
     */
    private Sqids createSqidsEncoder() {
        return Sqids.builder()
                .alphabet(ALPHABET)
                .minLength(MIN_CODE_LENGTH)
                .build();
    }

    /**
     * 初始化雪花ID生成器
     *
     * @return Snowflake实例
     * @throws RuntimeException 如果初始化失败
     */
    private Snowflake initSnowflake() {
        try {
            String clusterNodeName = clusterSystem.nodeConfig.getName();
            long datacenterId = calculateDatacenterId(clusterNodeName);
            long workerId = allocateWorkerId(clusterNodeName);
            log.info("雪花ID生成器初始化成功 -> workerId={}, datacenterId={}, host={}",
                    workerId, datacenterId, clusterNodeName);
            return new Snowflake(workerId, datacenterId);
        } catch (Exception e) {
            throw new RuntimeException("雪花ID生成器初始化失败", e);
        }
    }

    /**
     * 根据集群节点名称计算数据中心ID
     *
     * @param clusterNodeName 集群节点名称
     * @return 数据中心ID（0~31）
     */
    private long calculateDatacenterId(String clusterNodeName) {
        return Math.abs(clusterNodeName.hashCode()) % (MAX_DATACENTER_ID + 1);
    }

    /**
     * 为集群节点分配Worker ID
     * 如果该节点已分配过，则直接返回；否则分配新的ID
     *
     * @param clusterNodeName 集群节点名称
     * @return Worker ID（0~31）
     */
    private long allocateWorkerId(String clusterNodeName) {
        RMap<String, Long> workerIdMap = redissonClient.getMap(WORKER_ID_MAP_KEY);
        RLock lock = redissonClient.getLock(WORKER_ID_LOCK_KEY);
        lock.lock();
        try {
            // 检查是否已分配过Worker ID
            if (workerIdMap.containsKey(clusterNodeName)) {
                long existingWorkerId = workerIdMap.get(clusterNodeName);
                log.info("使用已有的Worker ID: {} -> {}", clusterNodeName, existingWorkerId);
                return existingWorkerId;
            }
            // 分配新的Worker ID
            long newWorkerId = generateNewWorkerId();
            workerIdMap.put(clusterNodeName, newWorkerId);
            log.info("为节点分配新的Worker ID: {} -> {}", clusterNodeName, newWorkerId);
            return newWorkerId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 生成新的Worker ID
     * 使用Redis原子计数器确保全局唯一性
     *
     * @return 新的Worker ID
     */
    private long generateNewWorkerId() {
        RAtomicLong counter = redissonClient.getAtomicLong(WORKER_ID_COUNTER_KEY);
        long workerId = counter.getAndIncrement();
        // 如果超过最大值，进行取模运算
        if (workerId > MAX_WORKER_ID) {
            workerId = workerId % (MAX_WORKER_ID + 1);
            counter.set(workerId + 1);
        }
        return workerId;
    }

    /**
     * 获取下一个唯一ID
     *
     * @return 雪花ID
     */
    public synchronized long nextId() {
        return snowflake.nextId();
    }

    /**
     * 将雪花ID编码为混淆字符串
     *
     * @param id 雪花ID
     * @return 编码后的字符串
     */
    public String encode(long id) {
        return sqids.encode(List.of(id));
    }

    /**
     * 将混淆字符串解码为雪花ID
     *
     * @param str 编码后的字符串
     * @return 雪花ID
     */
    public long decode(String str) {
        return sqids.decode(str).getFirst();
    }

    /**
     * 生成唯一的领奖码
     * 生成后会异步保存到数据库
     *
     * @param playerId 玩家ID
     * @param type     领奖码类型
     * @return 编码后的领奖码字符串
     */
    public String generateCode(long playerId, AwardCodeType type) {
        long snowflakeId = snowflake.nextId();
        String encodedCode = encode(snowflakeId);

        AwardCode awardCode = createAwardCode(playerId, type, snowflakeId, encodedCode);
        saveAwardCodeAsync(awardCode, playerId, encodedCode);

        return encodedCode;
    }

    /**
     * 创建领奖码数据对象
     *
     * @param playerId    玩家ID
     * @param type        领奖码类型
     * @param snowflakeId 雪花ID
     * @param encodedCode 编码后的领奖码
     * @return AwardCode对象
     */
    private AwardCode createAwardCode(long playerId, AwardCodeType type, long snowflakeId, String encodedCode) {
        AwardCode awardCode = new AwardCode();
        awardCode.setPlayerId(playerId);
        awardCode.setType(type);
        awardCode.setCreateTime(System.currentTimeMillis());
        awardCode.setSnowflakeId(snowflakeId);
        awardCode.setCode(encodedCode);
        return awardCode;
    }

    /**
     * 异步保存领奖码到数据库
     * 使用虚拟线程进行异步处理
     *
     * @param awardCode 领奖码对象
     * @param playerId  玩家ID
     * @param code      领奖码字符串
     */
    private void saveAwardCodeAsync(AwardCode awardCode, long playerId, String code) {
        Thread.ofVirtual().start(() -> {
            try {
                awardCodeDao.save(awardCode);
            } catch (Exception e) {
                log.error("保存领奖码失败, playerId={}, code={}, data={}",
                        playerId, code, awardCode, e);
            }
        });
    }
}
