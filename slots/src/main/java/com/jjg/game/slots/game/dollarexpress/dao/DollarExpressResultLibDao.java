package com.jjg.game.slots.game.dollarexpress.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/6/23 10:54
 */
@Repository
public class DollarExpressResultLibDao extends AbstractResultLibDao<DollarExpressResultLib> {

    public DollarExpressResultLibDao(@Autowired MongoTemplate mongoTemplate) {
        super(DollarExpressResultLib.class, mongoTemplate);
    }
}
