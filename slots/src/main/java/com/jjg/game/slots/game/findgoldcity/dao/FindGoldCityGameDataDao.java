package com.jjg.game.slots.game.findgoldcity.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityPlayerGameData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/8/27 10:46
 */
@Repository
public class FindGoldCityGameDataDao extends MongoBaseDao<FindGoldCityPlayerGameData, Long> {
    public FindGoldCityGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(FindGoldCityPlayerGameData.class, mongoTemplate);
    }
}
