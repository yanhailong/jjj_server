package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import org.bson.Document;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 11
 * @date 2025/7/10 17:38
 */
public abstract class AbstractResultLibDao<T extends SlotsResultLib> extends MongoBaseDao<T, Long> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    //这个表存储的是当前使用的是mongodb中哪个库(记录表名)
    protected final String slotsCurrentMongoResultLib = "slotsCurrentMongoResultLib";
    //这个表存储的是当前使用的是redis中哪个库(记录表名)
    protected final String slotsCurrentRedisResultLib = "slotsCurrentRedisResultLib";

    //redis记录数据本身
    protected final String slotsResultLib1 = "slotsResultLib1:";
    protected final String slotsResultLib2 = "slotsResultLib2:";

    private int batchSize = 500;

    //当前正在使用的结果库
    protected String currentMongoLibName;
    //当前正在使用的结果库
    protected String currentRedisLibName;

    //生成结果集的时候要加锁
    protected String generateLock = "generateLock";

    @Autowired
    protected RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redisson;

    private int gameType;

    public AbstractResultLibDao(Class<T> clazz, MongoTemplate mongoTemplate) {
        super(clazz, mongoTemplate);
    }

    public void init(int gameType) {
        this.gameType = gameType;
        reloadLib();
    }

    //加载最新的结果库名
    public void reloadLib() {
        this.currentMongoLibName = getCurrentMongoLibNameFromRedis();
        this.currentRedisLibName = getCurrentRedisLibNameFromRedis();
    }

    protected String generateLockTableName(int gameType) {
        return generateLock + ":" + gameType;
    }

    protected String tabelName(String tableIndex, int gameType, int libType, int sectionIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(tableIndex).append(gameType).append(":").append(libType).append(":").append(sectionIndex);
        return sb.toString();
    }

    protected String getRedisTableNameIndex(String existTableName) {
        if (existTableName == null || existTableName.isEmpty()) {
            return slotsResultLib1;
        }

        if (this.slotsResultLib1.equals(existTableName)) {
            return slotsResultLib2;
        }
        return slotsResultLib1;
    }

    protected String getMongodbTableNameIndex(String existTableName) {
        if (existTableName == null || existTableName.isEmpty()) {
            return slotsResultLib1;
        }

        if (this.slotsResultLib1.equals(existTableName)) {
            return slotsResultLib2;
        }
        return slotsResultLib1;
    }

    /**
     * 获取一个新的结果库名
     *
     * @return
     */
    public String getNewMongoLibName() {
        //获取正在使用的结果库(mongodb)
        String currentMongoLibName = getCurrentMongoLibNameFromRedis();
        int index = 1;
        if (currentMongoLibName != null && !currentMongoLibName.isEmpty()) {
            String[] arr = currentMongoLibName.split("_");
            int oldIndex = Integer.parseInt(arr[1]);
            index = oldIndex == 1 ? 2 : 1;
        }
//        this.newMongoDocName = this.clazz.getSimpleName() + "_" + index;
        return this.clazz.getSimpleName() + "_" + index;
    }

    /**
     * 获取当前正在使用的结果库
     *
     * @return
     */
    public String getCurrentMongoLibNameFromRedis() {
        return (String) this.redisTemplate.opsForHash().get(slotsCurrentMongoResultLib, this.gameType);
    }

    /**
     * 获取当前正在使用的结果库
     *
     * @return
     */
    public String getCurrentRedisLibNameFromRedis() {
        return (String) this.redisTemplate.opsForHash().get(slotsCurrentRedisResultLib, this.gameType);
    }

    public String getCurrentMongoLibName() {
        return currentMongoLibName;
    }

    public String getCurrentRedisLibName() {
        return currentRedisLibName;
    }

    /**
     * 加载到redis
     */
    public String moveToRedis(String docName, Map<Integer, Map<Integer, int[]>> resultLibSectionMap) {
        Query query = new Query();

        query.cursorBatchSize(batchSize);

        //获取当前正在使用的库名
        String redisTableName = getCurrentRedisLibNameFromRedis();
        //获取一个新的库名
        String redisTableNameIndex = getRedisTableNameIndex(redisTableName);

        int pipelineBatchSize = 300; // 每300条执行一次Pipeline
        List<Object> libBatch = new ArrayList<>(pipelineBatchSize);

        try (Stream<T> stream = mongoTemplate.stream(query, this.clazz, docName)) {
            Iterator<T> iterator = stream.iterator();
            while (iterator.hasNext()) {
                T lib = iterator.next();
                libBatch.add(lib);

                if (libBatch.size() >= pipelineBatchSize) {
                    executePipelineBatch(redisTableNameIndex, libBatch, resultLibSectionMap);
                    libBatch.clear();
                }
            }

            // 处理剩余记录
            if (!libBatch.isEmpty()) {
                executePipelineBatch(redisTableNameIndex, libBatch, resultLibSectionMap);
            }
        }

        //保存新的库名
        this.redisTemplate.opsForHash().put(slotsCurrentMongoResultLib, this.gameType, docName);
        this.redisTemplate.opsForHash().put(slotsCurrentRedisResultLib, this.gameType, redisTableNameIndex);

        this.currentMongoLibName = docName;
        this.currentRedisLibName = redisTableNameIndex;
        return redisTableNameIndex;
    }

    protected int getSectionIndex(Map<Integer, Map<Integer, int[]>> resultLibSectionMap, int libType, long times) {
        Map<Integer, int[]> libTypeMap = resultLibSectionMap.get(libType);
        if (libTypeMap == null) {
            return -1;
        }
        for (Map.Entry<Integer, int[]> en : libTypeMap.entrySet()) {
            int[] arr = en.getValue();
            if (times >= arr[0] && times < arr[1]) {
                return en.getKey();
            }
        }
        return -1;
    }

    /**
     * 清除mongo结果库
     */
    public void clearMongoLib() {
        clearMongoLib(this.currentMongoLibName);
    }

    /**
     * 清除mongo结果库
     */
    public void clearMongoLib(String docName) {
        if (docName == null || docName.isEmpty()) {
            log.debug("从mongo删除结果库失败，docName 为空");
            return;
        }

        String[] arr = docName.split("_");
        int index = Integer.parseInt(arr[1]);
        String removeName;
        if (index == 1) {
            removeName = arr[0] + "_2";
        } else {
            removeName = arr[0] + "_1";
        }
        this.mongoTemplate.dropCollection(removeName);
        log.debug("从mongodb移除结果库 removeName = {}", removeName);
    }

    /**
     * 清除redis结果库
     */
    public void clearRedisLib(int gameType) {
        clearRedisLib(this.currentRedisLibName, gameType);
    }

    /**
     * 清除redis结果库
     */
    public void clearRedisLib(String redisLibName, int gameType) {
        if (redisLibName == null || redisLibName.isEmpty()) {
            log.debug("从redis删除结果库失败，redisLibName 为空");
            return;
        }

        String removeName = this.slotsResultLib1.equals(redisLibName)
                ? this.slotsResultLib2
                : this.slotsResultLib1;

        String gameTableName = removeName + gameType;
        RKeys keys = redisson.getKeys();
        long start = System.currentTimeMillis();
        long deleted = keys.deleteByPattern(gameTableName + "*");
        log.debug("从redis移除结果库 removeName = {}, 删除Key数量 = {},耗时 = {} ms", gameTableName, deleted, System.currentTimeMillis() - start);
    }

    public T getLibBySectionIndex(int libType, int sectionIndex, Class<T> clazz) {
        String tableName = tabelName(this.currentRedisLibName, this.gameType, libType, sectionIndex);

        //根据条件随机获取一个结果库id
        Object object = this.redisTemplate.opsForSet().randomMember(tableName);
        if (object == null) {
            return null;
        }
        return mongoTemplate.findById(object.toString(), clazz, this.currentMongoLibName);
    }

    /**
     * 生成结果库的时候要添加所
     *
     * @param gameType
     * @return
     */
    public boolean addGenerateLock(int gameType) {
        return this.redisTemplate.opsForValue().setIfAbsent(generateLockTableName(gameType), true, 10, TimeUnit.MINUTES);
    }

    /**
     * 获取是否加锁
     *
     * @param gameType
     * @return
     */
    public boolean getGenerateLock(int gameType) {
        Object o = this.redisTemplate.opsForValue().get(generateLockTableName(gameType));
        if (o == null) {
            return false;
        }
        return Boolean.parseBoolean(o.toString());
    }

    public void removeGenerateLock(int gameType) {
        this.redisTemplate.delete(generateLockTableName(gameType));
    }

    /**
     * 一次性保存多条结果
     *
     * @param list
     * @return
     */
    public int batchSave(List<T> list, String docName) {
        BulkOperations bulkOps = this.mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, docName);
        for (T lib : list) {
            bulkOps.insert(lib);
        }
        return bulkOps.execute().getInsertedCount();
    }

    private void executePipelineBatch(String redisTableNameIndex,
                                      List<Object> batch,
                                      Map<Integer, Map<Integer, int[]>> resultLibSectionMap) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Object lib : batch) {
                T t = (T) lib;
                Set<Integer> libTypeSet = t.getLibTypeSet();
                for (int type : libTypeSet) {
                    int sectionIndex = getSectionIndex(resultLibSectionMap, type, t.getTimes());
                    if (sectionIndex < 0) continue;
                    connection.sAdd(
                            tabelName(redisTableNameIndex, gameType, type, sectionIndex).getBytes(),
//                            redisTemplate.getValueSerializer().serialize(t)

                            //保存整个对象会占用很大内存，所以这里只保存id
                            redisTemplate.getValueSerializer().serialize(t.getId())
                    );
                }
            }
            return null;
        });
    }
}
