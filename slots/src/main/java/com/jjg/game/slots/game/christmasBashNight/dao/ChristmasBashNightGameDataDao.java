package com.jjg.game.slots.game.christmasBashNight.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class ChristmasBashNightGameDataDao extends AbstractGameDataDao<ChristmasBashNightPlayerGameDataDTO> {
    public ChristmasBashNightGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(ChristmasBashNightPlayerGameDataDTO.class, mongoTemplate);
    }
}
