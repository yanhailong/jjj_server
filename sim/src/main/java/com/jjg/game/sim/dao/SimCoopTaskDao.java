package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.data.CoopSettlementReceipt;
import com.jjg.game.sim.data.SimCoopTaskData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * 多人协作任务数据 DAO (主键 = playerId, 范式对齐 {@link SimTaskDao})。
 *
 * @author 11
 * @date 2026/7/6
 */
@Repository
public class SimCoopTaskDao extends MongoBaseDao<SimCoopTaskData, Long> {
    public SimCoopTaskDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimCoopTaskData.class, mongoTemplate);
    }

    /**
     * 条件原子更新: 仅当任务处于 IN_ROOM 态时置为终态 (发起者离线的结算直写路径)。
     * 相比读改写全文档, 避免与退出登录时的全量保存竞态互相覆盖。
     *
     * @return 是否有文档被更新 (false = 数据不存在或状态不符, 视为未结算)
     */
    public boolean settleEntryIfInRoom(long playerId, int taskId, long roomId, int status, long finishTime) {
        Query query = settleQuery(playerId, taskId, roomId);
        CoopSettlementReceipt receipt = new CoopSettlementReceipt(taskId, status, finishTime);
        Update update = new Update()
                .set("tasks." + taskId + ".status", status)
                .set("tasks." + taskId + ".finishTime", finishTime)
                .set("settlementReceipts." + roomId, receipt);
        return mongoTemplate.updateFirst(query, update, SimCoopTaskData.class).getModifiedCount() > 0;
    }

    static Query settleQuery(long playerId, int taskId, long roomId) {
        return new Query(Criteria.where("_id").is(playerId)
                .and("tasks." + taskId + ".status").is(CoopTaskConst.TaskStatus.IN_ROOM)
                .and("tasks." + taskId + ".roomId").is(roomId)
                .and("settlementReceipts." + roomId).exists(false));
    }

    public boolean isEntrySettled(long playerId, int taskId, long roomId, int status) {
        Query query = new Query(Criteria.where("_id").is(playerId)
                .and("settlementReceipts." + roomId + ".taskId").is(taskId)
                .and("settlementReceipts." + roomId + ".status").is(status));
        return mongoTemplate.exists(query, SimCoopTaskData.class);
    }

    public void saveAll(Collection<SimCoopTaskData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SimCoopTaskData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SimCoopTaskData data : list) {
            bulk.replaceOne(new Query(Criteria.where("_id").is(data.getPlayerId())), data, upsert);
        }
        bulk.execute();
    }
}
