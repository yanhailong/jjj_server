package com.jjg.game.slots.game.zeusVsHades.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/8/27 10:46
 */
@Repository
public class ZeusVsHadesGameDataDao extends AbstractGameDataDao<ZeusVsHadesPlayerGameDataDTO> {
    public ZeusVsHadesGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(ZeusVsHadesPlayerGameDataDTO.class, mongoTemplate);
    }
}
