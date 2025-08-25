package com.jjg.game.core.dao;

import com.jjg.game.core.data.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

    /**
     * 根据玩家id查询
     * @return
     */
    public Account queryAccountByPlayerId(long playerId){
        return mongoTemplate.findById(playerId,this.clazz);
    }

    /**
     * 更新手机号
     * @param playerId
     * @param phoneNumber
     * @return
     */
    public boolean updatePhoneNumber(long playerId,String phoneNumber){
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("phoneNumber", phoneNumber);
        return mongoTemplate.updateFirst(query,update,this.clazz).getModifiedCount() > 0;
    }

    /**
     * 更新邮箱
     * @param playerId
     * @param email
     * @return
     */
    public boolean updateEmail(long playerId,String email){
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("email", email);
        return mongoTemplate.updateFirst(query,update,this.clazz).getModifiedCount() > 0;
    }

    /**
     * 根据注册mac查询
     * @return
     */
    public Account queryByRegisterMac(String registerMac){
        Query query = new Query(Criteria.where("registerMac").is(registerMac));
        return mongoTemplate.findOne(query,this.clazz);
    }

    /**
     * 根据登录mac查询
     * @return
     */
    public Account queryByLoginMac(String loginMac){
        Query query = new Query(Criteria.where("lastLoginMac").is(loginMac));
        return mongoTemplate.findOne(query,this.clazz);
    }

    /**
     * 根据手机号查询
     * @return
     */
    public Account queryByPhone(String phoneNumber){
        Query query = new Query(Criteria.where("phoneNumber").is(phoneNumber));
        return mongoTemplate.findOne(query,this.clazz);
    }

    /**
     * 根据邮箱查询
     * @return
     */
    public Account queryByEmail(String email){
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query,this.clazz);
    }

    /**
     * 修改账号状态
     * @param playerId
     * @param status
     * @return
     */
    public boolean updateAccountStatus(long playerId,int status){
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("status", status);
        return mongoTemplate.updateFirst(query,update,this.clazz).getModifiedCount() > 0;
    }
}
