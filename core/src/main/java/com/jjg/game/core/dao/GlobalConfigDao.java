package com.jjg.game.core.dao;

import com.jjg.game.core.data.GlobalConfig;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2026/4/23 18:30
 */
@Repository
public class GlobalConfigDao extends MongoBaseDao<GlobalConfig, Integer> {

    public GlobalConfigDao(MongoTemplate mongoTemplate) {
        super(GlobalConfig.class, mongoTemplate);
    }
}
