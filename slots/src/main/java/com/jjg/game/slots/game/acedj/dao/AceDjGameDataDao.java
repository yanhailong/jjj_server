package com.jjg.game.slots.game.acedj.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.acedj.data.AceDjPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class AceDjGameDataDao extends AbstractGameDataDao<AceDjPlayerGameDataDTO> {
    public AceDjGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(AceDjPlayerGameDataDTO.class, mongoTemplate);
    }
}
