package com.jjg.game.slots.game.panJinLian.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class PanJinLianGameDataDao extends AbstractGameDataDao<PanJinLianPlayerGameDataDTO> {
    public PanJinLianGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(PanJinLianPlayerGameDataDTO.class, mongoTemplate);
    }
}
