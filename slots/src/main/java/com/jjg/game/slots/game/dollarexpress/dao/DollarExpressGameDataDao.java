package com.jjg.game.slots.game.dollarexpress.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 16:48
 */
@Repository
public class DollarExpressGameDataDao extends AbstractGameDataDao<DollarExpressPlayerGameDataDTO> {
    public DollarExpressGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(DollarExpressPlayerGameDataDTO.class, mongoTemplate);
    }
}
