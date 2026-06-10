package com.jjg.game.social.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.manager.SocialManager;
import com.jjg.game.social.pb.req.*;
import com.jjg.game.social.pb.res.ResConversationList;
import com.jjg.game.social.pb.res.ResDeleteConversation;
import com.jjg.game.social.service.ChatService;
import com.jjg.game.social.service.FriendService;
import com.jjg.game.social.service.PlayerCardInfoService;
import com.jjg.game.social.service.PrivateChatService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 社交(好友 + 聊天)消息处理器。
 * <p>
 * 只读重 IO 接口(列表/历史/搜索/信息卡)经虚拟线程执行后直接回包, 不占用玩家绑定的Disruptor worker 线程
 * 这些接口每次同步串多次 Mongo/Redis 往返, 留在 worker 上会阻塞同槽位其他玩家的全部消息。
 * 写路径(发消息/好友关系变更/领礼)仍在 worker 上串行执行, 保持同一玩家操作的顺序与内存态安全。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SOCIAL)
public class SocialMessageHandler implements GmListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //只读请求执行器: 虚拟线程按任务创建, 实际并发由 Mongo/Redis 连接池约束
    private final ExecutorService readExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("social-read-", 0).factory());

    @Autowired
    private ChatService chatService;
    @Autowired
    private FriendService friendService;
    @Autowired
    private PrivateChatService privateChatService;
    @Autowired
    private PlayerCardInfoService playerCardInfoService;
    @Autowired
    private SocialManager socialManager;

    @PreDestroy
    public void shutdownReadExecutor() {
        readExecutor.shutdown();
    }

    /**
     * 只读请求转虚拟线程执行后回包 (pc.send 底层是 Netty writeAndFlush, 线程安全)。
     */
    private void sendAsync(PlayerController pc, Supplier<Object> task) {
        readExecutor.execute(() -> {
            try {
                pc.send(task.get());
            } catch (Exception e) {
                log.error("社交只读请求异步处理失败 playerId={}", pc.playerId(), e);
            }
        });
    }

    // --------------------------- 聊天 ---------------------------

    /**
     * 发送聊天消息
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_SEND_CHAT)
    public void reqSendChat(PlayerController pc, ReqSendChat req) {
        pc.send(chatService.sendChat(pc, req.channel, req.targetId, req.content));
    }

    /**
     * 拉取聊天历史
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_PULL_CHAT_HISTORY)
    public void reqPullChatHistory(PlayerController pc, ReqPullChatHistory req) {
        sendAsync(pc, () -> chatService.pullHistory(pc.playerId(), req.channel, req.targetId, req.cursor));
    }

    // --------------------------- 好友 ---------------------------

    /**
     * 获取好友列表
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_FRIEND_LIST)
    public void reqFriendList(PlayerController pc, ReqFriendList req) {
        sendAsync(pc, () -> friendService.friendList(pc.playerId()));
    }

    /**
     * 搜索玩家
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_SEARCH_PLAYER)
    public void reqSearchPlayer(PlayerController pc, ReqSearchPlayer req) {
        sendAsync(pc, () -> friendService.search(pc.playerId(), req.playerId));
    }

    /**
     * 发送好友申请
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_ADD_FRIEND)
    public void reqAddFriend(PlayerController pc, ReqAddFriend req) {
        pc.send(friendService.addFriend(pc.getPlayer(), req.playerId));
    }

    /**
     * 收到的申请列表
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_REQUEST_LIST)
    public void reqRequestList(PlayerController pc, ReqRequestList req) {
        sendAsync(pc, () -> friendService.requestList(pc.playerId()));
    }

    /**
     * 处理申请 (同意/拒绝, 支持一键)
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_HANDLE_REQUEST)
    public void reqHandleRequest(PlayerController pc, ReqHandleRequest req) {
        pc.send(friendService.handleRequest(pc.playerId(), req.playerIds, req.agree));
    }

    /**
     * 删除好友 (支持一键)
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_DELETE_FRIEND)
    public void reqDeleteFriend(PlayerController pc, ReqDeleteFriend req) {
        pc.send(friendService.deleteFriend(pc.playerId(), req.playerIds));
    }

    /**
     * 黑名单操作 (拉黑/移除/一键移除)
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_BLACKLIST_OP)
    public void reqBlacklistOp(PlayerController pc, ReqBlacklistOp req) {
        pc.send(friendService.blacklistOp(pc.playerId(), req.op, req.playerIds));
    }

    /**
     * 黑名单列表
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_BLACKLIST_LIST)
    public void reqBlacklistList(PlayerController pc, ReqBlacklistList req) {
        sendAsync(pc, () -> friendService.blacklistList(pc.playerId()));
    }

    /**
     * 赠送礼物 (支持一键)
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_SEND_GIFT)
    public void reqSendGift(PlayerController pc, ReqSendGift req) {
        pc.send(friendService.sendGift(pc.playerId(), req.playerIds, req.all));
    }

    /**
     * 一键收送(领取好友赠送的礼物)
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_COLLECT_GIFT)
    public void reqCollectGift(PlayerController pc, ReqCollectGift req) {
        pc.send(friendService.collectGift(pc.playerId()));
    }

    // --------------------------- 玩家信息卡 ---------------------------

    /**
     * 玩家信息
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_PLAYER_CARD)
    public void reqPlayerCard(PlayerController pc, ReqPlayerCard req) {
        sendAsync(pc, () -> playerCardInfoService.card(pc.playerId(), req.targetId));
    }

    // --------------------------- 私聊会话 ---------------------------

    /**
     * 私聊会话列表
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_CONVERSATION_LIST)
    public void reqConversationList(PlayerController pc, ReqConversationList req) {
        sendAsync(pc, () -> {
            ResConversationList res = new ResConversationList(Code.SUCCESS);
            res.list = privateChatService.conversationList(pc.playerId());
            return res;
        });
    }

    /**
     * 删除私聊会话
     *
     * @param pc
     * @param req
     */
    @Command(SocialConst.MsgBean.REQ_DELETE_CONVERSATION)
    public void reqDeleteConversation(PlayerController pc, ReqDeleteConversation req) {
        privateChatService.deleteConversation(pc.playerId(), req.targetId);
        ResDeleteConversation res = new ResDeleteConversation(Code.SUCCESS);
        res.targetId = req.targetId;
        pc.send(res);
    }

    // --------------------------- GM (联调测试) ---------------------------

    @Override
    public CommonResult<String> gm(PlayerController pc, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            String cmd = gmOrders[0];
            if ("socialSys".equalsIgnoreCase(cmd)) {
                socialManager.sendSystemMessage(gmOrders[1]);
            } else if ("socialWorld".equalsIgnoreCase(cmd)) {
                ReqSendChat req = new ReqSendChat();
                req.channel = ChatChannelType.WORLD.getCode();
                req.content = gmOrders[1];
                reqSendChat(pc, req);
            } else if ("socialFriends".equalsIgnoreCase(cmd)) {
                reqFriendList(pc, null);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
