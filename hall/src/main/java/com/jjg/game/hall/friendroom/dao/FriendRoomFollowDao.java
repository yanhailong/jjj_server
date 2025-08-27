package com.jjg.game.hall.friendroom.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.friendroom.constant.FriendRoomConstant;
import com.jjg.game.hall.friendroom.data.FriendRoomFollowBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    public FriendRoomFollowDao(@Autowired MongoTemplate mongoTemplate) {
        super(FriendRoomFollowBean.class, mongoTemplate);
    }

    /**
     * 获取默认的好友列表，默认第一页
     */
    public List<FriendRoomFollowBean> getDefualtRoomFriendList(long playerId, int invitationCode) {
        return getRoomFriendList(playerId, invitationCode, 0, FriendRoomConstant.PAGE_SIZE);
    }

    /**
     * 获取房间好友列表
     */
    public List<FriendRoomFollowBean> getRoomFriendList(long playerId, int invitationCode, int pageNum, int pageSize) {
        return mongoTemplate.find(
            Query.query(
                    Criteria.where("playerId").is(playerId)
                        .and("invitationCode").is(invitationCode)
                        .and("removeTime").is(0)
                )
                .with(Pageable.ofSize(pageSize).withPage(pageNum))
                .with(
                    Sort.by(
                        Sort.Order.desc("topUpTimeStamp"),
                        Sort.Order.asc("followedTimeStamp")
                    ))
            ,
            FriendRoomFollowBean.class
        );
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
    public void deleteMappingRelateByInvitationCode(int invitationCode) {
        mongoTemplate.updateMulti(
            Query.query(Criteria.where("invitationCode").is(invitationCode)),
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
