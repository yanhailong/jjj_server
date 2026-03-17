package com.jjg.game.core.dao.redeemcode;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.RedeemCodeInfo;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2026/3/12 15:40
 */
@Repository
public class RedeemCodeInfoDao extends MongoBaseDao<RedeemCodeInfo, Long> {
    public RedeemCodeInfoDao(MongoTemplate mongoTemplate) {
        super(RedeemCodeInfo.class, mongoTemplate);
    }
}
