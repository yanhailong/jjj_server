package com.jjg.game.alliance.dao;

import com.jjg.game.alliance.data.AllianceApplication;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AllianceHelpOrder;
import com.jjg.game.alliance.data.AllianceMember;
import com.jjg.game.alliance.data.AllianceTaskSlot;
import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 联盟数据 DAO。
 * <p>
 * 联盟是全服共享数据且 hall 多节点部署, 全部写操作走 Mongo 字段级条件原子更新
 * (与 {@code FriendDao} 同范式): 防超员靠 memberCount 条件、防重复接取靠 $pull 命中数、
 * 防重复帮助靠 helpers 存在性条件、任务池补齐靠 taskRefreshHour 旧值匹配。
 * 调用方根据返回的命中/修改数判定竞争结果, 失败方按业务回滚或重读。
 *
 * @author 11
 * @date 2026/6/11
 */
@Repository
public class AllianceDao extends MongoBaseDao<AllianceData, Long> {

    public AllianceDao(@Autowired MongoTemplate mongoTemplate) {
        super(AllianceData.class, mongoTemplate);
    }

    private Query byId(long allianceId) {
        return new Query(Criteria.where("_id").is(allianceId));
    }

    // ----------------------- 成员 -----------------------

    /**
     * 原子加成员: 仅当联盟存在、未满员、该玩家尚不在盟中时成功; 顺带移除其入盟申请。
     *
     * @param cap 当前等级对应的人数上限 (调用方按缓存联盟等级计算; 等级只升不降, cap 偏小最多导致
     *            "升级瞬间少进一人"的保守误差, 不会超员)
     * @return true 加入成功
     */
    public boolean tryAddMember(long allianceId, long playerId, AllianceMember member, int cap) {
        Query query = new Query(Criteria.where("_id").is(allianceId)
                .and("memberCount").lt(cap)
                .and("members." + playerId).exists(false));
        Update update = new Update()
                .inc("memberCount", 1)
                .set("members." + playerId, member)
                .unset("applications." + playerId);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    /**
     * 原子移除成员 (退出/被踢)。盟主不可被移除 (需先转让)。
     *
     * @return true 移除成功
     */
    public boolean removeMember(long allianceId, long playerId) {
        Query query = new Query(Criteria.where("_id").is(allianceId)
                .and("members." + playerId).exists(true)
                .and("leaderId").ne(playerId));
        Update update = new Update()
                .inc("memberCount", -1)
                .unset("members." + playerId);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    /**
     * 转让盟主: 仅当操作者当前是盟主且目标在盟内时成功。
     */
    public boolean transferLeader(long allianceId, long fromId, long toId) {
        Query query = new Query(Criteria.where("_id").is(allianceId)
                .and("leaderId").is(fromId)
                .and("members." + toId).exists(true));
        Update update = new Update()
                .set("leaderId", toId)
                .set("members." + toId + ".position", com.jjg.game.alliance.constant.AllianceConst.Position.LEADER)
                .set("members." + fromId + ".position", com.jjg.game.alliance.constant.AllianceConst.Position.MEMBER);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    /**
     * 解散联盟: 仅盟主可删, 物理删除文档。
     *
     * @return true 删除成功
     */
    public boolean dissolve(long allianceId, long leaderId) {
        Query query = new Query(Criteria.where("_id").is(allianceId).and("leaderId").is(leaderId));
        return mongoTemplate.remove(query, AllianceData.class).getDeletedCount() > 0;
    }

    /**
     * 更新成员活跃时间 (登录时调用, 供对决"活跃人数"模式统计)。
     */
    public void touchMemberActive(long allianceId, long playerId, long time) {
        Query query = new Query(Criteria.where("_id").is(allianceId)
                .and("members." + playerId).exists(true));
        mongoTemplate.updateFirst(query,
                new Update().set("members." + playerId + ".lastActiveTime", time), AllianceData.class);
    }

    // ----------------------- 设置 -----------------------

    /**
     * 盟主编辑联盟信息 (条件 leaderId 校验权限, 防止转让竞态后旧盟主仍可改)。
     */
    public boolean updateSettings(long allianceId, long leaderId, String name, int icon,
                                  String notice, int joinMinCasinoLevel, boolean joinNeedAudit) {
        Query query = new Query(Criteria.where("_id").is(allianceId).and("leaderId").is(leaderId));
        Update update = new Update()
                .set("name", name)
                .set("icon", icon)
                .set("notice", notice)
                .set("joinMinCasinoLevel", joinMinCasinoLevel)
                .set("joinNeedAudit", joinNeedAudit);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    // ----------------------- 申请 -----------------------

    /**
     * 写入入盟申请: 仅当该玩家尚未申请且不在盟中时写入。
     * 申请列表上限由调用方预检 (宽松约束, 极端并发下略超上限可接受, 超期申请会被惰性清理)。
     */
    public boolean addApplication(long allianceId, long playerId, AllianceApplication application) {
        Query query = new Query(Criteria.where("_id").is(allianceId)
                .and("applications." + playerId).exists(false)
                .and("members." + playerId).exists(false));
        Update update = new Update().set("applications." + playerId, application);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    /**
     * 批量移除申请 (拒绝/超期清理/同意后兜底)。
     */
    public void removeApplications(long allianceId, Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        Update update = new Update();
        for (Long pid : playerIds) {
            update.unset("applications." + pid);
        }
        mongoTemplate.updateFirst(byId(allianceId), update, AllianceData.class);
    }

    // ----------------------- 声誉/等级 -----------------------

    /**
     * 原子累加声誉值并返回累加后的文档 (调用方据新值推导等级)。
     *
     * @return 累加后的联盟数据; 联盟不存在返回 null
     */
    public AllianceData incReputation(long allianceId, long delta) {
        //仅投影 reputation/level 回传: 调用方只据二者重算等级, 避免高频加声誉时把整份联盟文档
        //(members/applications/helpOrders 三个内嵌 map)随 findAndModify 回搬, 削减大盟网络开销
        Query query = byId(allianceId);
        query.fields().include("reputation").include("level");
        return mongoTemplate.findAndModify(query,
                new Update().inc("reputation", delta),
                FindAndModifyOptions.options().returnNew(true),
                AllianceData.class);
    }

    /**
     * 条件升级: 仅当当前等级低于 newLevel 时写入 (等级只升不降, 并发重算无害)。
     *
     * @return true 本次调用完成了升级 (用于触发升级广播, 多节点只有一个会成功)
     */
    public boolean tryUpgradeLevel(long allianceId, int newLevel) {
        Query query = new Query(Criteria.where("_id").is(allianceId).and("level").lt(newLevel));
        return mongoTemplate.updateFirst(query,
                new Update().set("level", newLevel), AllianceData.class).getModifiedCount() > 0;
    }

    // ----------------------- 任务池 -----------------------

    /**
     * 任务池整点补齐: 条件匹配旧的 taskRefreshHour, 多节点并发只有一个成功。
     * 池按 cfgId 索引 (内嵌对象, key=cfgId), 与字段类型 {@code Map<Integer,AllianceTaskSlot>} 一致。
     *
     * @param oldHour 读到的旧补齐整点
     * @param newHour 当前整点 (yyyyMMddHH)
     * @param tasks   补齐后的完整任务池 (cfgId -> slot)
     * @return true 本次调用完成了补齐
     */
    public boolean refreshTasks(long allianceId, long oldHour, long newHour, Map<Integer, AllianceTaskSlot> tasks) {
        Query query = new Query(Criteria.where("_id").is(allianceId).and("taskRefreshHour").is(oldHour));
        Update update = new Update().set("taskRefreshHour", newHour).set("tasks", tasks);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    /**
     * 原子摘取任务: 仅当 tasks.{cfgId} 存在时移除该条目, 接取成功者独占。
     * 并发接取同一 cfgId 时, 只有第一个匹配 exists 条件并 $unset 成功, 其余命中数为 0。
     *
     * @return true 摘取成功
     */
    public boolean pullTask(long allianceId, int taskCfgId) {
        Query query = new Query(Criteria.where("_id").is(allianceId)
                .and("tasks." + taskCfgId).exists(true));
        Update update = new Update().unset("tasks." + taskCfgId);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    // ----------------------- 互助 -----------------------

    /**
     * 写入求助订单。
     */
    public void addHelpOrder(long allianceId, AllianceHelpOrder order) {
        mongoTemplate.updateFirst(byId(allianceId),
                new Update().set("helpOrders." + order.getOrderId(), order), AllianceData.class);
    }

    /**
     * 原子帮助: 仅当订单存在且该玩家尚未帮助过时记录。
     * 注: 订单帮助次数上限由调用方按缓存预检 + 完成后清单兜底, 此处不再做 $size 条件
     * (Mongo 对 map 大小的条件表达开销大, 极端并发下多帮一次属可接受误差)。
     *
     * @return true 帮助成功
     */
    public boolean tryHelp(long allianceId, long orderId, long helperId, long time, int maxHelp) {
        if (maxHelp <= 0) {
            return false;
        }
        String path = "helpOrders." + orderId;
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(allianceId),
                Criteria.where(path).exists(true),
                Criteria.where(path + ".helpers." + helperId).exists(false),
                new Criteria().orOperator(
                        Criteria.where(path + ".helpCount").exists(false),
                        Criteria.where(path + ".helpCount").lt(maxHelp))));
        Update update = new Update()
                .set(path + ".helpers." + helperId, time)
                .inc(path + ".helpCount", 1);
        return mongoTemplate.updateFirst(query, update, AllianceData.class).getModifiedCount() > 0;
    }

    /**
     * 批量移除求助订单 (完成/超时清理)。
     */
    @Deprecated
    public boolean tryHelp(long allianceId, long orderId, long helperId, long time) {
        return tryHelp(allianceId, orderId, helperId, time, Integer.MAX_VALUE);
    }

    public void removeHelpOrders(long allianceId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        Update update = new Update();
        for (Long oid : orderIds) {
            update.unset("helpOrders." + oid);
        }
        mongoTemplate.updateFirst(byId(allianceId), update, AllianceData.class);
    }

    // ----------------------- 查询 -----------------------

    /**
     * 可加入联盟列表: 按声誉降序取一页 (服务层再按"等级门槛/满员"过滤)。
     * 只投影列表展示所需字段, 不拉 members/tasks 等大字段。
     */
    public void removeHelpOrderIfFull(long allianceId, long orderId, int maxHelp) {
        if (maxHelp <= 0) {
            return;
        }
        String path = "helpOrders." + orderId;
        Query query = new Query(Criteria.where("_id").is(allianceId).and(path + ".helpCount").gte(maxHelp));
        mongoTemplate.updateFirst(query, new Update().unset(path), AllianceData.class);
    }

    /**
     * 按声誉降序取联盟概要 (可加入列表/一键加入扫描用)。
     * 额外投影当前玩家自己的申请条目, 供列表展示"是否已申请过"。
     */
    public List<AllianceData> listByReputation(int limit, long playerId) {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "reputation"));
        query.limit(limit);
        query.fields().include("name", "icon", "level", "reputation",
                "memberCount", "members", "joinMinCasinoLevel", "joinNeedAudit", "leaderId");
        query.fields().include("applications." + playerId);
        return mongoTemplate.find(query, AllianceData.class);
    }

    /**
     * 批量取联盟概要 (排行榜展示用, 同样只投影概要字段)。
     */
    public List<AllianceData> multiGetBrief(Collection<Long> allianceIds) {
        if (allianceIds == null || allianceIds.isEmpty()) {
            return List.of();
        }
        Query query = new Query(Criteria.where("_id").in(allianceIds));
        query.fields().include("name", "icon", "level", "reputation", "memberCount", "members", "leaderId");
        return mongoTemplate.find(query, AllianceData.class);
    }

    /**
     * 分页取全部联盟 id (leader 周榜结算遍历用; 联盟数量级远小于玩家, 分页扫描可控)。
     */
    public List<Long> pageIds(long lastId, int limit) {
        Query query = new Query(Criteria.where("_id").gt(lastId));
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(limit);
        query.fields().include("_id");
        return mongoTemplate.find(query, AllianceData.class).stream()
                .map(AllianceData::getAllianceId).toList();
    }
}
