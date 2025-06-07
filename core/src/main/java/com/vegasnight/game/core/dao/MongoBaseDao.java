package com.vegasnight.game.core.dao;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

import java.io.Serializable;

/**
 * mongo db 数据访问基类
 * @author 11
 * @date 2025/5/26 17:04
 */
public abstract class MongoBaseDao<T, ID extends Serializable> extends SimpleMongoRepository<T, ID> {
    protected MongoTemplate mongoTemplate;
    protected Class<T> clazz;

    public MongoBaseDao(Class<T> clazz, MongoTemplate mongoTemplate) {
        super(new MongoRepositoryFactory(mongoTemplate)
                .getEntityInformation(clazz), mongoTemplate);
        this.mongoTemplate = mongoTemplate;
        this.clazz = clazz;
    }

    @Override
    public <S extends T> S save(S entity) {
        return super.save(entity);
    }
}