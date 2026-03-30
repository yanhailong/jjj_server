package com.jjg.game.slots.game.angrybirds.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/8/27 10:46
 */
@Repository
public class AngryBirdsGameDataDao extends AbstractGameDataDao<AngryBirdsPlayerGameDataDTO> {
    public AngryBirdsGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(AngryBirdsPlayerGameDataDTO.class, mongoTemplate);
    }
}
