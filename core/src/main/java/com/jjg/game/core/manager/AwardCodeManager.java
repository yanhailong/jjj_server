package com.jjg.game.core.manager;

import cn.hutool.core.lang.Snowflake;
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

import java.net.InetAddress;
import java.util.List;

/**
 * 领奖码生成器 使用前必须要调用init方法初始化雪花id生成器
 */
@Component
public class AwardCodeManager {

    private final static Logger log = LoggerFactory.getLogger(AwardCodeManager.class);

    private final static String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String WORKER_ID_MAP_KEY = "snowflake:workerId:map";
    private static final String WORKER_ID_COUNTER_KEY = "snowflake:workerId:counter";
    private static final String WORKER_ID_LOCK_KEY = "snowflake:workerId:lock";
    // 0~31
    private static final long MAX_WORKER_ID = 31;
    private static final long MAX_DATACENTER_ID = 31;


    /**
     * 领奖码的最小长度
     */
    private final static int MIN_LENGTH = 10;
    /**
     * 雪花id编解码器
     */
    private final Sqids sqids;
    /**
     * 雪花ID生成器实例
     */
    private final Snowflake idWorker;
    /**
     * 领奖码dao
     */
    private final AwardCodeDao awardCodeDao;
    private final RedissonClient redissonClient;


    public AwardCodeManager(AwardCodeDao awardCodeDao, RedissonClient redissonClient) {
        this.awardCodeDao = awardCodeDao;
        this.redissonClient = redissonClient;

        this.sqids = Sqids.builder()
                .alphabet(ALPHABET)
                .minLength(MIN_LENGTH)
                .build();
        this.idWorker = initSnowflake();
        log.info("AwardCodeManager init success!");
    }

    /**
     * 初始化雪花id生成器
     */
    private Snowflake initSnowflake() {
        long workerId;
        long datacenterId;
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            datacenterId = Math.abs(hostname.hashCode()) % (MAX_DATACENTER_ID + 1);
            RMap<String, Long> workerIdMap = redissonClient.getMap(WORKER_ID_MAP_KEY);
            RLock lock = redissonClient.getLock(WORKER_ID_LOCK_KEY);
            lock.lock();
            try {
                // 如果该主机名已经分配过 workerId，则直接使用
                if (workerIdMap.containsKey(hostname)) {
                    workerId = workerIdMap.get(hostname);
                    log.info("使用已有 workerId: {} -> {}", hostname, workerId);
                } else {
                    // 分配新的 workerId
                    RAtomicLong counter = redissonClient.getAtomicLong(WORKER_ID_COUNTER_KEY);
                    workerId = counter.getAndIncrement();
                    if (workerId > MAX_WORKER_ID) {
                        workerId = workerId % (MAX_WORKER_ID + 1);
                        counter.set(workerId + 1);
                    }
                    workerIdMap.put(hostname, workerId);
                    log.info("为主机分配新 workerId: {} -> {}", hostname, workerId);
                }
            } finally {
                lock.unlock();
            }
            log.info("Snowflake 初始化成功 -> workerId={}, datacenterId={}, host={}",
                    workerId, datacenterId, hostname);
            return new Snowflake(workerId, datacenterId);
        } catch (Exception e) {
            throw new RuntimeException("初始化 Snowflake 失败", e);
        }
    }

    /**
     * 获取一个唯一id
     */
    public synchronized long nextId() {
        return idWorker.nextId();
    }

    /**
     * 加密为随机字符串
     *
     * @param id 雪花id
     * @return 加密后的字符串
     */
    public String encode(long id) {
        return sqids.encode(List.of(id));
    }

    /**
     * 将加密后的字符串还原为雪花id
     *
     * @param str 加密后的字符串
     * @return 雪花id
     */
    public long decode(String str) {
        return sqids.decode(str).getFirst();
    }

    /**
     * 生成一个唯一的领奖码 创建好以后会保存到数据库中
     *
     * @return 包含字母和数字的唯一领奖码
     */
    public String generateCode(long playerId, AwardCodeType type) {
        AwardCode awardCode = new AwardCode();
        awardCode.setPlayerId(playerId);
        awardCode.setType(type);
        awardCode.setCreateTime(System.currentTimeMillis());
        long id = idWorker.nextId();
        awardCode.setSnowflakeId(id);
        String encodedId = encode(id);
        awardCode.setCode(encodedId);
        Thread.ofVirtual().start(() -> {
            try {
                awardCodeDao.save(awardCode);
            } catch (Exception e) {
                log.error("保存领奖码失败, playerId={}, code={}, data= {}", playerId, encodedId, awardCode, e);
            }
        });
        return encodedId;
    }

}
