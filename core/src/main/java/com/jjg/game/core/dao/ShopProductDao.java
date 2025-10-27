package com.jjg.game.core.dao;

import com.jjg.game.core.data.ShopProduct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/17 20:54
 */
@Repository
public class ShopProductDao extends MongoBaseDao<ShopProduct, Integer>{
    private Logger log = LoggerFactory.getLogger(getClass());

    public ShopProductDao(MongoTemplate mongoTemplate) {
        super(ShopProduct.class, mongoTemplate);
    }

    /**
     * 获取所有开启状态的商品
     * @return
     */
    public List<ShopProduct> getAll() {
        Query query = new Query(Criteria.where("open").is(true));
        return mongoTemplate.find(query,ShopProduct.class);
    }

    /**
     * 根据id删除
     * @param delIds
     */
    public void delById(List<Long> delIds) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, ShopProduct.class);
        for (long id : delIds) {
            Query query = new Query(Criteria.where("_id").is(id));
            bulkOps.remove(query);
        }
        bulkOps.execute();
    }


    /**
     * 保存
     * @param products
     */
    public void saveProducts(List<ShopProduct> products) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, ShopProduct.class);

        products.forEach(product -> {
            Query query = Query.query(Criteria.where("_id").is(product.getId()));
            bulkOps.replaceOne(query, product, FindAndReplaceOptions.options().upsert());
        });

        bulkOps.execute(); // 一次性提交
    }
}
