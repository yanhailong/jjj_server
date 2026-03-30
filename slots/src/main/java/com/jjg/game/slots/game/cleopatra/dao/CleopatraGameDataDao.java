package com.jjg.game.slots.game.cleopatra.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/27 10:48
 */
@Repository
public class CleopatraGameDataDao extends AbstractGameDataDao<CleopatraPlayerGameDataDTO> {
    public CleopatraGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(CleopatraPlayerGameDataDTO.class, mongoTemplate);
    }
}
