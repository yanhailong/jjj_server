package com.jjg.game.slots.game.frozenThrone.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThronePlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class FrozenThroneGameDataDao extends AbstractGameDataDao<FrozenThronePlayerGameDataDTO> {
    public FrozenThroneGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(FrozenThronePlayerGameDataDTO.class, mongoTemplate);
    }
}
