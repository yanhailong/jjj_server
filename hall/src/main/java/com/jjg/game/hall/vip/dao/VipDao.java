package com.jjg.game.hall.vip.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.vip.data.Vip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/8/27 10:03
 */
@Repository
public class VipDao extends MongoBaseDao<Vip, Long> {
    public VipDao(@Autowired MongoTemplate mongoTemplate) {
        super(Vip.class, mongoTemplate);
    }
}
