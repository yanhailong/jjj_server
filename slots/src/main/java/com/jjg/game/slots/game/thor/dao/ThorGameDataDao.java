package com.jjg.game.slots.game.thor.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.thor.data.ThorPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Repository
public class ThorGameDataDao extends AbstractGameDataDao<ThorPlayerGameDataDTO> {
    public ThorGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(ThorPlayerGameDataDTO.class, mongoTemplate);
    }
}
