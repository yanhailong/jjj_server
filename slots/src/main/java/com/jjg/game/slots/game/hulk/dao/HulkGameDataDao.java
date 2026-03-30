package com.jjg.game.slots.game.hulk.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.hulk.data.HulkPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2026/1/15
 */
@Repository
public class HulkGameDataDao extends AbstractGameDataDao<HulkPlayerGameDataDTO> {
    public HulkGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(HulkPlayerGameDataDTO.class, mongoTemplate);
    }
}
