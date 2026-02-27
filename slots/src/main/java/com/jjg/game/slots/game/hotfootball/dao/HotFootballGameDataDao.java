package com.jjg.game.slots.game.hotfootball.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.hotfootball.data.HotFootballPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/27 10:46
 */
@Repository
public class HotFootballGameDataDao extends AbstractGameDataDao<HotFootballPlayerGameDataDTO> {
    public HotFootballGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(HotFootballPlayerGameDataDTO.class, mongoTemplate);
    }
}
