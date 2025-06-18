package com.jjg.game.account.dao;

import com.jjg.game.account.entity.Account;
import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/5/24 17:39
 */
@Repository
public class AccountDao extends MongoBaseDao<Account,Long> {

    public AccountDao(@Autowired MongoTemplate mongoTemplate) {
        super(Account.class, mongoTemplate);
    }

    /**
     * 根据游客账号查询
     * @param guest
     * @return
     */
    public Account queryAccountByGuest(String guest){
        Query query = new Query(Criteria.where("guest").is(guest));
        return mongoTemplate.findOne(query,this.clazz);
    }
}
