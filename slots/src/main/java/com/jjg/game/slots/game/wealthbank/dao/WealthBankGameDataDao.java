package com.jjg.game.slots.game.wealthbank.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.wealthbank.data.WealthBankPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/27 10:43
 */
@Repository
public class WealthBankGameDataDao extends AbstractGameDataDao<WealthBankPlayerGameDataDTO> {
    public WealthBankGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(WealthBankPlayerGameDataDTO.class, mongoTemplate);
    }
}