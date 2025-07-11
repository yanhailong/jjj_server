package com.jjg.game.slots.game.dollarexpress.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author 11
 * @date 2025/6/23 10:54
 */
@Repository
public class DollarExpressResultLibDao extends AbstractResultLibDao<DollarExpressResultLib> {

    public DollarExpressResultLibDao(@Autowired MongoTemplate mongoTemplate) {
        super(DollarExpressResultLib.class, mongoTemplate);
    }

    /**
     * 一次性保存多条结果
     * @param list
     * @return
     */
    public int batchSave(List<DollarExpressResultLib> list,String docName){
        BulkOperations bulkOps = this.mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, docName);
        for (DollarExpressResultLib lib : list) {
            bulkOps.insert(lib);
        }
        return bulkOps.execute().getInsertedCount();
    }

}
