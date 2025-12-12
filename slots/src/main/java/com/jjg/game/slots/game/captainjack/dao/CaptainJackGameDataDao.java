package com.jjg.game.slots.game.captainjack.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/27 10:46
 */
@Repository
public class CaptainJackGameDataDao extends AbstractGameDataDao<CaptainJackPlayerGameDataDTO> {
    public CaptainJackGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(CaptainJackPlayerGameDataDTO.class, mongoTemplate);
    }
}
