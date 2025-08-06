package com.jjg.game.slots.game.mahjiongwin.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:39
 */
@Repository
public class MahjiongWinGameDataDao extends AbstractGameDataDao<MahjiongWinPlayerGameDataDTO> {
    public MahjiongWinGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(MahjiongWinPlayerGameDataDTO.class, mongoTemplate);
    }
}
