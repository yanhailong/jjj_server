package com.jjg.game.alliance.dao;

import com.jjg.game.alliance.data.AllianceBattleData;
import com.jjg.game.alliance.data.BattleResult;
import com.jjg.game.alliance.data.BattleSignup;
import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * 联盟对决期数据 DAO。
 * <p>
 * 阶段推进全部用"旧状态条件更新"幂等化: leader 切换或并发 tick 时只有一个节点能完成迁移,
 * 报名/匹配/结算写入都挂在对应状态条件之下。
 *
 * @author 11
 * @date 2026/6/11
 */
@Repository
public class AllianceBattleDao extends MongoBaseDao<AllianceBattleData, String> {

    public AllianceBattleDao(@Autowired MongoTemplate mongoTemplate) {
        super(AllianceBattleData.class, mongoTemplate);
    }

    /**
     * 创建本期文档 (不存在才插入; 并发创建一个成功即可)。
     *
     * @return true 本次调用完成了创建
     */
    public boolean insertIfAbsent(AllianceBattleData battle) {
        try {
            mongoTemplate.insert(battle);
            return true;
        } catch (DuplicateKeyException ignore) {
            return false;
        }
    }

    /**
     * 条件推进阶段: 仅当当前状态等于 fromState 时迁移到 toState。
     *
     * @return true 本次调用完成了迁移
     */
    public boolean tryAdvanceState(String period, int fromState, int toState) {
        Query query = new Query(Criteria.where("_id").is(period).and("state").is(fromState));
        return mongoTemplate.updateFirst(query,
                new Update().set("state", toState), AllianceBattleData.class).getModifiedCount() > 0;
    }

    /**
     * 报名: 仅在报名阶段且该联盟尚未报名时写入。
     *
     * @return true 报名成功
     */
    public boolean addSignup(String period, long allianceId, BattleSignup signup, int signupState) {
        Query query = new Query(Criteria.where("_id").is(period)
                .and("state").is(signupState)
                .and("signups." + allianceId).exists(false));
        Update update = new Update().set("signups." + allianceId, signup);
        return mongoTemplate.updateFirst(query, update, AllianceBattleData.class).getModifiedCount() > 0;
    }

    /**
     * 写入匹配结果并推进状态 (条件 fromState, 与匹配执行同一原子操作)。
     */
    public boolean saveMatchesAndAdvance(String period, int fromState, int toState, Map<Long, Long> matches) {
        Query query = new Query(Criteria.where("_id").is(period).and("state").is(fromState));
        Update update = new Update().set("state", toState).set("matches", matches);
        return mongoTemplate.updateFirst(query, update, AllianceBattleData.class).getModifiedCount() > 0;
    }

    /**
     * 写入全部结算结果并推进到已结算 (条件 fromState)。
     */
    public boolean saveResultsAndAdvance(String period, int fromState, int toState, Map<Long, BattleResult> results) {
        Query query = new Query(Criteria.where("_id").is(period).and("state").is(fromState));
        Update update = new Update().set("state", toState).set("results", results);
        return mongoTemplate.updateFirst(query, update, AllianceBattleData.class).getModifiedCount() > 0;
    }
}
