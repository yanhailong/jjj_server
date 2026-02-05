package com.jjg.game.slots.dao;

import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.utils.LZ4CompressionUtil;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/7/10 17:38
 */
public abstract class AbstractResultLibDao<T extends SlotsResultLib> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected Class<T> clazz;

    //这个表存储的是当前使用的是redis中哪个库(记录表名)
    protected final String slotsCurrentRedisResultLib = "slotsCurrentRedisResultLib";

    //redis记录数据本身
    protected final String slotsResultLib1 = "slotsResultLib1:";
    protected final String slotsResultLib2 = "slotsResultLib2:";

    //当前正在使用的结果库
    protected String currentRedisLibName;

    //生成结果集的时候要加锁
    protected String generateLock = "generateLock:";

    //记录最后一次生成结果库的时间
    protected String lastGenLibTime = "lastGenLibTime";

    @Autowired
    protected RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redisson;

    private int gameType;


    public AbstractResultLibDao(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void init(int gameType) {
        this.gameType = gameType;
        reloadLib();
    }

    //加载最新的结果库名
    public void reloadLib() {
        this.currentRedisLibName = getCurrentRedisLibNameFromRedis();
    }

    protected String generateLockTableName(int gameType) {
        return generateLock + gameType;
    }

    protected String tabelName(String tableIndex, int gameType, int libType, int sectionIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(tableIndex).append(gameType).append(":").append(libType).append(":").append(sectionIndex);
        return sb.toString();
    }

    public String getNewRedisTableName() {
        //获取当前正在使用的库名
        String redisTableName = getCurrentRedisLibNameFromRedis();
        //获取一个新的库名
        return getRedisTableNameIndex(redisTableName);
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

    /**
     * 获取当前正在使用的结果库
     *
     * @return
     */
    public String getCurrentRedisLibNameFromRedis() {
        return (String) this.redisTemplate.opsForHash().get(slotsCurrentRedisResultLib, this.gameType);
    }

    public String getCurrentRedisLibName() {
        return currentRedisLibName;
    }


    /**
     * 批量保存到redis
     */
    public void batchSaveToRedis(String redisTableNameIndex, List<T> libList, Map<Integer, Map<Integer, int[]>> resultLibSectionMap) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (T lib : libList) {
                Set<Integer> libTypeSet = lib.getLibTypeSet();
                byte[] data = ProtostuffUtil.serialize(lib);
                byte[] compressData = LZ4CompressionUtil.compressFast(data);

                ByteBuffer buffer = ByteBuffer.allocate(4 + compressData.length);
                buffer.putInt(data.length);
                buffer.put(compressData);

                for (int type : libTypeSet) {
                    int sectionIndex = getSectionIndex(resultLibSectionMap, type, lib.getTimes());
                    if (sectionIndex < 0) continue;
                    connection.sAdd(
                            tabelName(redisTableNameIndex, gameType, type, sectionIndex).getBytes(),
                            buffer.array()
                    );
                }
            }
            return null;
        });
    }

    public void afterSave(String newRedisTableName) {
        this.redisTemplate.opsForHash().put(slotsCurrentRedisResultLib, this.gameType, newRedisTableName);
        this.currentRedisLibName = newRedisTableName;
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
        log.debug("从redis移除旧的结果库 gameType = {},removeName = {}, 删除Key数量 = {},耗时 = {} ms", gameType, gameTableName, deleted, System.currentTimeMillis() - start);
    }

    /**
     * 根据倍数区间获取结果库对象
     *
     * @param libType
     * @param sectionIndex
     * @param clazz
     * @return
     */
    public T getLibBySectionIndex(int libType, int sectionIndex, Class<T> clazz) {
        String tableName = tabelName(this.currentRedisLibName, this.gameType, libType, sectionIndex);

        // 使用RedisCallback直接读取二进制数据
        byte[] compressedData = (byte[]) redisTemplate.execute(
                (RedisCallback<byte[]>) connection ->
                        connection.sRandMember(tableName.getBytes())
        );

        return deserializeResultLib(compressedData, clazz);
    }

    /**
     * 获取一个不中奖的结果
     *
     * @param clazz
     * @return
     */
    public T getNoWinLib(Class<T> clazz) {
        return getLibBySectionIndex(1, 0, clazz);
    }

    /**
     * 反序列化结果库数据
     */
    public T deserializeResultLib(byte[] compressedData, Class<T> clazz) {
        if (compressedData == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(compressedData);
        int originalLength = buffer.getInt();  // 读取原始长度

        byte[] data = new byte[compressedData.length - 4];
        buffer.get(data);

        compressedData = LZ4CompressionUtil.decompressFast(data, originalLength);
        //获取结果后进行解压反序列化
        return ProtostuffUtil.deserialize(compressedData, clazz);
    }

    /**
     * 生成结果库的时候要添加所
     *
     * @param gameType
     * @return
     */
    public boolean addGenerateLock(int gameType) {
        return this.redisTemplate.opsForValue().setIfAbsent(generateLockTableName(gameType), true, 30, TimeUnit.MINUTES);
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
     * 修改最后一次生成结果库的时间
     * @param gameType
     */
    public void addGenerateTime(int gameType) {
        this.redisTemplate.opsForHash().put(lastGenLibTime,gameType, System.currentTimeMillis());
    }
}
