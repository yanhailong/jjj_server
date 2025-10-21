package com.jjg.game.hall.friendroom.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.hall.friendroom.constant.FriendRoomConstant;
import com.jjg.game.hall.friendroom.data.FriendRoomFollowBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 房间好友Dao
 *
 * @author 2CL
 */
@Repository
public class FriendRoomFollowDao extends MongoBaseDao<FriendRoomFollowBean, Long> {

//    private static final Snowflake snowflake = new Snowflake(NodeType.HALL.getValue(), NodeType.HALL.getValue());

    private final SnowflakeManager snowflakeManager;

    public FriendRoomFollowDao(@Autowired MongoTemplate mongoTemplate, @Lazy SnowflakeManager snowflakeManager) {
        super(FriendRoomFollowBean.class, mongoTemplate);
        this.snowflakeManager = snowflakeManager;
    }

    /**
     * 获取默认的好友列表，默认第一页
     */
    public List<FriendRoomFollowBean> getDefualtRoomFriendList(long playerId) {
        return getRoomFriendList(playerId, 0, FriendRoomConstant.PAGE_SIZE);
    }

    /**
     * 获取房间好友列表
     */
    public List<FriendRoomFollowBean> getRoomFriendList(long playerId, int pageNum, int pageSize) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("playerId").is(playerId).and("removeTime").lte(0)),
            Aggregation.sort(Sort.by(
                Sort.Order.desc("followedTimeStamp")
            )),
            Aggregation.skip((long) pageNum * pageSize),
            Aggregation.limit(pageSize)
        );
        // 需要对查询进行优化
        return mongoTemplate.aggregate(aggregation, "friendRoomFollowBean", FriendRoomFollowBean.class).getMappedResults();
    }

    /**
     * 玩家好友数量
     */
    public long countRoomFriendSize(long playerId) {
        return mongoTemplate.count(
            Query.query(
                Criteria.where("playerId").is(playerId)
                    .and("removeTime").is(0)
            )
            ,
            FriendRoomFollowBean.class
        );
    }

    /**
     * 批量软删除关注玩家
     */
    public void removeFollowedFriend(Collection<Long> removeId) {
        mongoTemplate.updateMulti(
            Query.query(Criteria.where("id").in(removeId)),
            Update.update("removeTime", System.currentTimeMillis()),
            FriendRoomFollowBean.class
        );
    }

    /**
     * 通过邀请码软删除所有映射关系
     */
    public void deleteMappingRelateByInvitationCode(long targetPlayerId, int invitationCode) {
        mongoTemplate.updateMulti(
            Query.query(
                Criteria.where("invitationCode").is(invitationCode)
                    .and("followedPlayerId").is(targetPlayerId)
            ),
            Update.update("removeTime", System.currentTimeMillis()),
            FriendRoomFollowBean.class
        );
    }

    /**
     * 获取房间好友
     */
    public FriendRoomFollowBean getRoomFriend(long playerId, long targetPlayerId, int invitationCode) {
        return mongoTemplate.findOne(
            Query.query(
                Criteria.where("playerId").is(playerId)
                    .and("invitationCode").is(invitationCode)
                    .and("followedPlayerId").is(targetPlayerId)
                    .and("removeTime").is(0)
            ),
            FriendRoomFollowBean.class
        );
    }

    /**
     * 通过邀请码关注好友
     */
    public FriendRoomFollowBean addFriendByInvitationCode(long playerId, long followedPlayerId, int invitationCode) {
        FriendRoomFollowBean friendRoomFollowBean = new FriendRoomFollowBean();
        friendRoomFollowBean.setId(snowflakeManager.nextId());
        friendRoomFollowBean.setFollowedPlayerId(followedPlayerId);
        friendRoomFollowBean.setPlayerId(playerId);
        friendRoomFollowBean.setInvitationCode(invitationCode);
        friendRoomFollowBean.setFollowedTimeStamp(System.currentTimeMillis());
        return mongoTemplate.insert(friendRoomFollowBean);
    }

    /**
     * 更新好友关注bean
     */
    public FriendRoomFollowBean updateFriendRoomFollowBean(FriendRoomFollowBean friendRoomFollowBean) {
        return mongoTemplate.save(friendRoomFollowBean);
    }
}
