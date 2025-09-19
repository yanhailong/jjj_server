package com.jjg.game.hall.minigame.game.luckytreasure.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.minigame.game.luckytreasure.data.LuckyTreasureConfig;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 夺宝奇兵配置dao
 */
@Repository
public class LuckTreasureConfigDao extends MongoBaseDao<LuckyTreasureConfig, Integer> {

    public LuckTreasureConfigDao(MongoTemplate mongoTemplate) {
        super(LuckyTreasureConfig.class, mongoTemplate);
    }

    /**
     * 根据ID查询配置
     *
     * @param id 配置ID
     * @return 配置对象
     */
    public LuckyTreasureConfig findById(int id) {
        return mongoTemplate.findById(id, LuckyTreasureConfig.class);
    }

    /**
     * 根据ID删除配置
     *
     * @param id 配置ID
     * @return 删除的记录数
     */
    public long deleteById(int id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.remove(query, LuckyTreasureConfig.class).getDeletedCount();
    }

    /**
     * 替换配置（没有就新增，有就修改）
     *
     * @param config 配置对象
     * @return 保存后的配置对象
     */
    public LuckyTreasureConfig replace(LuckyTreasureConfig config) {
        return mongoTemplate.save(config);
    }

    /**
     * 批量替换配置（没有就新增，有就修改）
     *
     * @param configs 配置列表
     * @return 保存后的配置列表
     */
    public List<LuckyTreasureConfig> batchReplace(List<LuckyTreasureConfig> configs) {
        return configs.stream()
                .map(this::replace)
                .toList();
    }

    /**
     * 批量删除配置（按ID列表）
     *
     * @param ids 配置ID列表
     * @return 删除的记录数
     */
    public long batchDeleteByIds(List<Integer> ids) {
        Query query = new Query(Criteria.where("id").in(ids));
        return mongoTemplate.remove(query, LuckyTreasureConfig.class).getDeletedCount();
    }

}
