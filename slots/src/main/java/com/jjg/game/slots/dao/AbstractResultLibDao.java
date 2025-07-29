package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsResultLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

    private int batchSize = 1000;

    //当前正在使用的结果库
    protected String currentMongoLibName;
    //当前正在使用的结果库
    protected String currentRedisLibName;

    //生成结果集的时候要加锁
    protected String generateLock = "generateLock";

    @Autowired
    protected RedisTemplate redisTemplate;

    private int gameType;

    public AbstractResultLibDao(Class<T> clazz, MongoTemplate mongoTemplate) {
        super(clazz, mongoTemplate);
    }

    public void init(int gameType){
        this.gameType = gameType;
        reloadLib();
    }

    //加载最新的结果库名
    public void reloadLib(){
        this.currentMongoLibName = getCurrentMongoLibNameFromRedis();
        this.currentRedisLibName = getCurrentRedisLibNameFromRedis();
    }

    protected String generateLockTableName(int gameType){
        return generateLock + ":" + gameType;
    }

    protected String tabelName(String tableIndex, int gameType, int modelId, int libType, int sectionIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(tableIndex).append(gameType).append(":").append(modelId).append(":").append(libType).append(":").append(sectionIndex);
        return sb.toString();
    }
    protected String allSectionTabelName(String tableIndex, int gameType, int modelId, int libType) {
        return tableIndex + gameType + ":" + modelId + ":" + libType;
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
     *
     */
    public String moveToRedis(String docName, Map<Integer, Map<Integer, Map<Integer, int[]>>> resultLibSectionMap) {
        Query query = new Query();

        query.cursorBatchSize(batchSize);

        //获取当前正在使用的库名
        String redisTableName = getCurrentRedisLibNameFromRedis();
        //获取一个新的库名
        String redisTableNameIndex = getRedisTableNameIndex(redisTableName);

        //使用游标处理数据
        try (Stream<T> stream = mongoTemplate.stream(query, this.clazz, docName)) {
            stream.forEach(lib -> {
                int sectionIndex = getSectionIndex(resultLibSectionMap, lib.getRollerMode(), lib.getLibType(), lib.getTimes());
                if (sectionIndex < 0) {
                    log.warn("将结果库转移到redis时失败，获取区间失败 gameType = {},modelId = {},libType = {},times = {},libId = {}", this.gameType, lib.getRollerMode(), lib.getLibType(), lib.getTimes(),lib.getId());
                    return;
                }
                this.redisTemplate.opsForSet().add(tabelName(redisTableNameIndex, this.gameType, lib.getRollerMode(), lib.getLibType(), sectionIndex), lib);
//                    this.redisTemplate.opsForSet().add(allSectionTabelName(redisTableNameIndex, this.gameType, lib.getRollerMode(), lib.getLibType()), sectionIndex);
            });
        }

        //保存新的库名
        this.redisTemplate.opsForHash().put(slotsCurrentMongoResultLib, this.gameType, docName);
        this.redisTemplate.opsForHash().put(slotsCurrentRedisResultLib, this.gameType, redisTableNameIndex);

        this.currentMongoLibName = docName;
        this.currentRedisLibName = redisTableNameIndex;
        return redisTableNameIndex;
    }

    protected int getSectionIndex(Map<Integer, Map<Integer, Map<Integer, int[]>>> resultLibSectionMap, int modelId, int libType, int times) {
        Map<Integer, Map<Integer, int[]>> modelMap = resultLibSectionMap.get(modelId);
        if (modelMap == null) {
            return -1;
        }
        Map<Integer, int[]> libTypeMap = modelMap.get(libType);
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
        if(this.currentMongoLibName != null && !this.currentMongoLibName.isEmpty()){
            String[] arr = this.currentMongoLibName.split("_");
            int index = Integer.parseInt(arr[1]);
            String removeName;
            if(index == 1){
                removeName = arr[0] + "_2";
            }else {
                removeName = arr[0] + "_1";
            }
            this.mongoTemplate.dropCollection(removeName);
            log.debug("从mongodb移除结果库 removeName = {}", removeName);
        }else {
            log.debug("从mongo删除结果库失败，currentMongoLibName 为空");
        }
    }

    /**
     * 清除redis结果库
     */
    public void clearRedisLib(){
        if(this.currentRedisLibName != null && !this.currentRedisLibName.isEmpty()){
            String removeName;
            if(this.slotsResultLib1.equals(this.currentRedisLibName)){
                removeName = this.slotsResultLib2;
            }else {
                removeName = this.slotsResultLib1;
            }

            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(removeName + "*").count(1000).build())) {
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    String key = new String(keyBytes, StandardCharsets.UTF_8);
                    redisTemplate.delete(key);
                }
            }
            log.debug("从redis移除结果库 removeName = {}", removeName);
        }else {
            log.debug("从redis删除结果库失败，currentRedisLibName 为空");
        }
    }

    public T getLibBySectionIndex(int modelId, int libType, int sectionIndex) {
        String tableName = tabelName(this.currentRedisLibName, this.gameType, modelId, libType, sectionIndex);
        return (T)this.redisTemplate.opsForSet().randomMember(tableName);
    }

    public Set<Integer> getAllSection(int modelId, int libType){
        Set members = this.redisTemplate.opsForSet().members(allSectionTabelName(this.currentRedisLibName, this.gameType, modelId, libType));
        if(members == null || members.isEmpty()){
            return null;
        }
        Set<Integer> sections = new HashSet<>();
        for(Object o : members){
            sections.add(Integer.parseInt(o.toString()));
        }
        return sections;
    }

    /**
     * 生成结果库的时候要添加所
     * @param gameType
     * @return
     */
    public boolean addGenerateLock(int gameType){
        return this.redisTemplate.opsForValue().setIfAbsent(generateLockTableName(gameType),true,10, TimeUnit.MINUTES);
    }

    public void removeGenerateLock(int gameType){
        this.redisTemplate.delete(generateLockTableName(gameType));
    }

}
