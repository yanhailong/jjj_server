package com.jjg.game.slots.game.basketballSuperstar.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class BasketballSuperstarGameDataDao extends AbstractGameDataDao<BasketballSuperstarPlayerGameDataDTO> {
    public BasketballSuperstarGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(BasketballSuperstarPlayerGameDataDTO.class, mongoTemplate);
    }
}
