package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
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

    //记录数据本身
    protected final String slotsResultLib1 = "slotsResultLib1:";
    protected final String slotsResultLib2 = "slotsResultLib2:";

    private int batchSize = 1000;

    //当前正在使用的结果库
    protected String currentMongoDocName;
    protected String newMongoDocName;

    @Autowired
    protected RedisTemplate redisTemplate;

    public AbstractResultLibDao(Class<T> clazz, MongoTemplate mongoTemplate) {
        super(clazz, mongoTemplate);
    }

    public long getResultCount(T lib) {
        //获取条数，非精确,但是高效
        return this.mongoTemplate.estimatedCount(currentMongoDocName);
    }

    public void removeTable(){
        this.mongoTemplate.dropCollection(currentMongoDocName);
    }

    protected String tabelName(String tableIndex,int gameType,int modelId,int libType,int sectionIndex){
        return tableIndex + gameType + ":" + modelId + ":" + libType + ":" + sectionIndex;
    }

    protected String getRedisTableNameIndex(String existTableName){
        if(existTableName == null || existTableName.isEmpty()){
            return slotsResultLib1;
        }

        if(this.slotsResultLib1.equals(existTableName)){
            return slotsResultLib2;
        }
        return slotsResultLib1;
    }

    /**
     * 获取一个新的结果库名
     * @return
     */
    public String getNewMongoLibName(int gameType){
        String docName = this.clazz.getSimpleName() + "_1";
        boolean put = this.redisTemplate.opsForHash().putIfAbsent(slotsCurrentMongoResultLib, gameType,docName);
        if(put){
            this.newMongoDocName = docName;
            return docName;
        }

        String name = this.redisTemplate.opsForHash().get(slotsCurrentMongoResultLib, gameType).toString();
        String[] arr = name.split("_");
        int index = Integer.parseInt(arr[1]);
        int newIndex = index == 1 ? 2 : 1;
        this.newMongoDocName = this.clazz.getSimpleName() + "_" + newIndex;
        return this.newMongoDocName;
    }

    /**
     * 获取当前正在使用的结果库
     * @param gameType
     * @return
     */
    public String getCurrentMongoLibName(int gameType){
        return  (String)this.redisTemplate.opsForHash().get(slotsCurrentMongoResultLib, gameType);
    }
    /**
     * 获取当前正在使用的结果库
     * @param gameType
     * @return
     */
    public String getCurrentRedisLibName(int gameType){
        return  (String)this.redisTemplate.opsForHash().get(slotsCurrentRedisResultLib, gameType);
    }

    /**
     * 加载到redis
     * @param gameType
     */
    public void moveToRedis(int gameType,String docName,Map<Integer, Map<Integer, Map<Integer,int[]>>> resultLibSectionMap){
        Query query = new Query();

        query.cursorBatchSize(batchSize);

        //获取当前正在使用的库名
        String redisTableName = getCurrentRedisLibName(gameType);
        //获取一个新的库名
        String redisTableNameIndex = getRedisTableNameIndex(redisTableName);

        //使用游标处理数据
        try (Stream<T> stream = mongoTemplate.stream(query, this.clazz,docName)) {
            stream.forEach(lib -> {
                int sectionIndex = getSectionIndex(resultLibSectionMap, lib.getRollerMode(), lib.getLibType(), lib.getTimes());
                if(sectionIndex < 0){
                    log.warn("将结果库转移到redis时失败，获取区间失败 gameType = {},modelId = {},libType = {},times = {}",gameType,lib.getRollerMode(), lib.getLibType(), lib.getTimes());
                    return;
                }

                this.redisTemplate.opsForSet().add(tabelName(redisTableNameIndex,gameType,lib.getRollerMode(),lib.getLibType(),sectionIndex),lib);
            });
        }

        //保存新的库名
        this.redisTemplate.opsForHash().put(slotsCurrentRedisResultLib, gameType,redisTableNameIndex);
    }

    protected int getSectionIndex(Map<Integer, Map<Integer,Map<Integer,int[]>>> resultLibSectionMap,int modelId,int libType,int times){
        Map<Integer, Map<Integer, int[]>> modelMap = resultLibSectionMap.get(modelId);
        if(modelMap == null){
            return -1;
        }
        Map<Integer, int[]> libTypeMap = modelMap.get(libType);
        if(libTypeMap == null){
            return -1;
        }
        for(Map.Entry<Integer,int[]> en : libTypeMap.entrySet()){
            int[] arr = en.getValue();
            if(times >= arr[0] && times < arr[1]){
                return en.getKey();
            }
        }
        return -1;
    }
}
