package com.jjg.game.activity.common.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.utils.RedisUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * redis记录操作类
 *
 * @author lm
 * @date 2025/9/22 17:46
 */
@Repository
public class RecordDao {
    private final Logger log = LoggerFactory.getLogger(RecordDao.class);
    // 单个玩家的记录 key:功能名:功能id:玩家id
    private final String RECORD_KEY = "activity:%s:record:%d:%d";
    // 全部玩家的记录 key:功能名:功能id
    private final String ALL_RECORD_KEY = "activity:%s:record:all:%d";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisUtils redisUtils;

    public RecordDao(RedisTemplate<String, String> redisTemplate, RedisUtils redisUtils) {
        this.redisTemplate = redisTemplate;
        this.redisUtils = redisUtils;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 删除所有玩家记录
     *
     * @param functionName 功能名称
     * @param id           功能id
     */
    public void deleteAllPlayerRecords(String functionName, long id) {
        String pattern = String.format("activity:%s:record:%d:*", functionName, id);
        redisUtils.deleteByPattern(redisTemplate, pattern);
    }

    /**
     * 删除所有全局记录
     *
     * @param functionName 功能名称
     * @param id           功能id
     */
    public void deleteAllRecords(String functionName, long id) {
        String allRecordKey = ALL_RECORD_KEY.formatted(functionName, id);
        redisTemplate.delete(allRecordKey);
    }



    public <T> void addRecord(String functionName, long id, long playerId, T record, int maxNum, boolean addAll) {
        String recordJson;
        try {
            recordJson = objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.error("addRecord error functionName:{} id:{} playerId:{}", functionName, id, playerId, e);
            return;
        }
        if (StringUtils.isEmpty(recordJson)) {
            log.error("addRecord isEmpty functionName:{} id:{} playerId:{}", functionName, id, playerId);
            return;
        }
        if (playerId > 0) {
            String key = RECORD_KEY.formatted(functionName, id, playerId);
            redisTemplate.opsForList().leftPush(key, recordJson);
            if (maxNum > 0) {
                redisTemplate.opsForList().trim(key, 0, maxNum - 1);
            }
        }
        if (!addAll) {
            return;
        }
        String allRecordKey = ALL_RECORD_KEY.formatted(functionName, id);
        redisTemplate.opsForList().leftPush(allRecordKey, recordJson);
        if (maxNum > 0) {
            redisTemplate.opsForList().trim(allRecordKey, 0, maxNum - 1);
        }
    }

    /**
     * 获取玩家记录
     *
     * @param functionName 功能名称
     * @param playerId     玩家id
     * @param id           功能id
     * @param start        开始索引
     * @param size         记录数
     * @param clazz        反序列化类
     * @param <T>          记录类
     * @return 记录(是否还有记录,记录数据)
     */
    public <T> Pair<Boolean, List<T>> getPlayerRecords(String functionName, long id, long playerId, int start, int size, Class<T> clazz) {
        String key = RECORD_KEY.formatted(functionName, id, playerId);
        try {
            return getRecords(key, start, size, clazz);
        } catch (Exception e) {
            log.error("获取全局记录 json解析失败 fieldName:{} id:{} playerId:{} ", functionName, id, playerId, e);
        }
        return Pair.newPair(false, List.of());
    }

    /**
     * 获取全局记录
     *
     * @param functionName 功能名称
     * @param id           功能id
     * @param start        开始索引
     * @param size         记录数
     * @param clazz        反序列化类
     * @param <T>          记录类
     * @return 记录(是否还有记录,记录数据)
     */
    public <T> Pair<Boolean, List<T>> getRecords(String functionName, long id, int start, int size, Class<T> clazz) {
        String redisKey = ALL_RECORD_KEY.formatted(functionName, id);
        try {
            return getRecords(redisKey, start, size, clazz);
        } catch (Exception e) {
            log.error("获取全局记录 json解析失败 fieldName:{} id:{} ", functionName, id, e);
        }
        return Pair.newPair(false, List.of());
    }

    /**
     * 删除玩家记录
     *
     * @param functionName 功能名称
     * @param id           功能id
     * @param playerId     玩家id
     */
    public void deletePlayerRecords(String functionName, long id, long playerId) {
        String key = RECORD_KEY.formatted(functionName, id, playerId);
        redisTemplate.delete(key);
    }

    /**
     * 获取记录
     *
     * @param redisKey redisKey
     * @param start    开始索引
     * @param size     记录数
     * @param clazz    反序列化类
     * @param <T>      记录类
     * @return 记录(是否还有记录,记录数据)
     */
    private <T> Pair<Boolean, List<T>> getRecords(String redisKey, int start, int size, Class<T> clazz) throws Exception {
        List<String> records = redisTemplate.opsForList().range(redisKey, start, start + size);
        if (records == null || records.isEmpty()) {
            return Pair.newPair(false, List.of());
        }
        boolean hasNext = false;
        if (records.size() > size) {
            records.removeLast();
            hasNext = true;
        }
        List<T> recordList = new ArrayList<>();
        for (String record : records) {
            recordList.add(objectMapper.readValue(record, clazz));
        }
        return Pair.newPair(hasNext, recordList);
    }
}
