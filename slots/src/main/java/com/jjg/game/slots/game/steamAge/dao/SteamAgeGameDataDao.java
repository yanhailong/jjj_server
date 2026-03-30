package com.jjg.game.slots.game.steamAge.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.steamAge.data.SteamAgePlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class SteamAgeGameDataDao extends AbstractGameDataDao<SteamAgePlayerGameDataDTO> {
    public SteamAgeGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(SteamAgePlayerGameDataDTO.class, mongoTemplate);
    }
}
