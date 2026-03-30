package com.jjg.game.slots.game.tigerbringsriches.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/27 10:46
 */
@Repository
public class TigerBringsRichesGameDataDao extends AbstractGameDataDao<TigerBringsRichesPlayerGameDataDTO> {
    public TigerBringsRichesGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(TigerBringsRichesPlayerGameDataDTO.class, mongoTemplate);
    }
}
