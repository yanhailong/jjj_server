package com.jjg.game.core.dao.luckytreasure;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.LuckyTreasure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 夺宝奇兵数据dao
 */
@Repository
public class LuckyTreasureDao extends MongoBaseDao<LuckyTreasure, Long> {

    public LuckyTreasureDao(MongoTemplate mongoTemplate) {
        super(LuckyTreasure.class, mongoTemplate);
    }

    /**
     * 根据endTime查询记录
     *
     * @param endTime 结束时间
     * @param limit   最大查询数量，0表示不限制
     * @return 指定endTime的LuckyTreasure列表
     */
    public List<LuckyTreasure> findAllByEndTime(long endTime, int limit) {
        Query query = new Query(Criteria.where("endTime").is(endTime));
        if (limit > 0) {
            query.limit(limit);
        }
        return mongoTemplate.find(query, LuckyTreasure.class);
    }

    /**
     * 分页查询指定玩家参与过的夺宝奇兵活动
     * 按照开始时间倒序排列
     *
     * @param playerId 玩家ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<LuckyTreasure> findPlayerRecord(long playerId, Pageable pageable) {
        // 构建查询条件：buyMap中包含指定玩家ID
        Query query = new Query(Criteria.where("buyMap." + playerId).exists(true));

        // 按开始时间倒序排序
        query.with(Sort.by("startTime").descending());

        // 查询总数（不分页）
        long total = mongoTemplate.count(query, LuckyTreasure.class);

        // 设置分页参数
        query.with(pageable);

        // 查询数据
        List<LuckyTreasure> treasures = mongoTemplate.find(query, LuckyTreasure.class);

        return new PageImpl<>(treasures, pageable, total);
    }


    /**
     * 分页查询所有已结束的夺宝奇兵活动（开奖历史记录）
     * 按照结束时间倒序排列
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<LuckyTreasure> findAllRewardHistory(Pageable pageable, int limit) {
        // 构建查询条件：endTime > 0 表示已结束
        Query query = new Query(Criteria.where("endTime").gt(0));

        // 按结束时间倒序排序
        query.with(pageable.getSort().and(Sort.by("endTime").descending()));

        // 设置分页参数
        query.with(pageable);

        // 查询数据
        List<LuckyTreasure> treasures = mongoTemplate.find(query, LuckyTreasure.class);

        // 查询总数
        Query countQuery = new Query(Criteria.where("endTime").gt(0));

        long total = mongoTemplate.count(countQuery, LuckyTreasure.class);

        if (limit > 0) {
            if (total > limit) {
                total = limit;
            }
        }

        return new PageImpl<>(treasures, pageable, total);
    }

}
