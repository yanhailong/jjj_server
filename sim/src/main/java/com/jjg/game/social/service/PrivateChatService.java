package com.jjg.game.social.service;

import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.social.channel.ChatHistory;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.dao.ConversationEntryDao;
import com.jjg.game.social.dao.FriendDao;
import com.jjg.game.social.dao.PrivateMessageDao;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.data.ConversationEntry;
import com.jjg.game.social.data.PrivateMessage;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.NotifyChat;
import com.jjg.game.social.pb.struct.ConversationInfo;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 私聊业务: 落库(7天TTL)、实时投递、会话列表、分页历史。
 * 为保证"刚发出、尚未刷盘"的消息也能被历史拉取到, {@link #loadHistory} 在拉首页时会合并写缓冲中的对应会话消息。
 * <p>
 * 写缓冲有界 ({@link SocialConst.Cfg#PRIVATE_WRITE_BUFFER_MAX}): 超限(通常是 Mongo 持续不可用)降级为同步单条落库,
 * 防止内存无界增长; 并按会话维护缓冲索引, 拉首页合并只扫本会话而非全量遍历。
 * 会话列表读 {@link ConversationEntry} 摘要表 (批量落库时增量维护), 不再扫消息表聚合。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class PrivateChatService {
    private static final Logger log = LoggerFactory.getLogger(PrivateChatService.class);

    //会话列表返回的最大会话数
    private static final int CONVERSATION_SCAN_LIMIT = 200;

    @Autowired
    private PrivateMessageDao dao;
    @Autowired
    private ConversationEntryDao conversationDao;
    @Autowired
    private FriendDao friendDao;
    @Autowired
    private SocialSender sender;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SocialStatusService statusService;

    //写缓冲: 待批量落库的消息
    private final ConcurrentLinkedQueue<PrivateMessage> writeBuffer = new ConcurrentLinkedQueue<>();
    //写缓冲大小 (ConcurrentLinkedQueue.size() 为 O(n), 单独计数)
    private final AtomicInteger bufferSize = new AtomicInteger();
    //会话id -> 缓冲中该会话的消息(id->消息): 供拉首页合并, 替代全量遍历写缓冲。仅在 compute/computeIfPresent 中修改
    private final ConcurrentHashMap<String, Map<Long, PrivateMessage>> bufferIndex = new ConcurrentHashMap<>();
    //同一时刻至多一个落库任务在执行 (防止 Mongo 持续缓慢时任务在执行器队列中无界积压)
    private final AtomicBoolean flushing = new AtomicBoolean();
    //批量落库 IO 线程
    private ExecutorService ioExecutor;
    //定时刷盘句柄
    private Timeout flushTimeout;

    public void init() {
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "social-private-io");
            t.setDaemon(true);
            return t;
        });
        //确保 TTL / 分页索引
        dao.ensureTtlIndex(SocialConst.Cfg.PRIVATE_KEEP_DAYS);
        conversationDao.ensureIndexes(SocialConst.Cfg.PRIVATE_KEEP_DAYS);
        //定时批量刷盘
        long intervalSec = SocialConst.Cfg.PRIVATE_FLUSH_INTERVAL_SEC;
        this.flushTimeout = WheelTimerUtil.scheduleAtFixedRate(this::flush, intervalSec, intervalSec, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (flushTimeout != null) {
            flushTimeout.cancel();
        }
        //关服前把缓冲剩余消息同步落库
        flushRemaining();
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            try {
                ioExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 进缓冲(异步批量落库) + 实时投递给在线对端 (由 PrivateChatChannel.dispatch 调用)。
     */
    public void store(ChatMessage msg) {
        PrivateMessage pm = new PrivateMessage();
        pm.setId(msg.getId());
        pm.setConversationId(PrivateMessage.conversationId(msg.getFromId(), msg.getToId()));
        pm.setFromId(msg.getFromId());
        pm.setToId(msg.getToId());
        pm.setContent(msg.getContent());
        pm.setTime(msg.getTime());
        pm.setCreateTime(new Date(msg.getTime()));
        pm.setRead(false);

        if (ioExecutor == null || bufferSize.get() >= SocialConst.Cfg.PRIVATE_WRITE_BUFFER_MAX) {
            //未初始化(非 hall 节点兜底) 或 缓冲已满(Mongo 持续不可用): 同步落库, 不再入缓冲
            storeDirect(pm);
        } else {
            //入写缓冲, 等定时任务批量落库
            enqueue(pm);
        }

        //在线对端实时收到
        NotifyChat notify = new NotifyChat(Code.SUCCESS);
        notify.msg = SocialPbConverter.toChatMsgInfo(msg);
        boolean send = sender.sendTo(msg.getToId(), notify);
        if (!send) {
            log.warn("发送私聊信息失败 playerId={},toId={}", msg.getFromId(), msg.getToId());
        }
    }

    /**
     * 先入会话索引再入队列: flush 抽到的消息必然已在索引中, 落库成功后可正确清除。
     */
    private void enqueue(PrivateMessage pm) {
        bufferIndex.compute(pm.getConversationId(), (k, m) -> {
            if (m == null) {
                m = new ConcurrentHashMap<>();
            }
            m.put(pm.getId(), pm);
            return m;
        });
        writeBuffer.offer(pm);
        bufferSize.incrementAndGet();
    }

    /**
     * 降级路径: 同步单条落库 + 维护摘要; 失败则放弃该条历史 (实时投递不受影响)。
     */
    private void storeDirect(PrivateMessage pm) {
        try {
            dao.insertMessage(pm);
            conversationDao.bulkApply(List.of(pm));
        } catch (Exception e) {
            log.error("私聊写缓冲不可用且同步落库失败, 该条消息不进历史 id={},conversationId={}",
                    pm.getId(), pm.getConversationId(), e);
        }
    }

    /**
     * 定时刷盘: 抽干写缓冲, 交 IO 线程批量 insert (单飞: 上一批未完成则跳过本周期)。
     */
    private void flush() {
        if (writeBuffer.isEmpty()) {
            return;
        }
        if (!flushing.compareAndSet(false, true)) {
            return;
        }
        List<PrivateMessage> batch = drainBuffer();
        if (batch.isEmpty()) {
            flushing.set(false);
            return;
        }
        ExecutorService executor = this.ioExecutor;
        if (executor == null) {
            try {
                safeInsert(batch);
            } finally {
                flushing.set(false);
            }
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    safeInsert(batch);
                } finally {
                    flushing.set(false);
                }
            });
        } catch (Exception e) {
            //执行器已关闭等异常: 同步兜底
            try {
                safeInsert(batch);
            } finally {
                flushing.set(false);
            }
        }
    }

    /**
     * 关服同步刷盘 (不经 IO 线程, 确保返回前已落库)。
     */
    private void flushRemaining() {
        List<PrivateMessage> batch = drainBuffer();
        safeInsert(batch);
    }

    private List<PrivateMessage> drainBuffer() {
        List<PrivateMessage> batch = new ArrayList<>();
        PrivateMessage pm;
        while ((pm = writeBuffer.poll()) != null) {
            batch.add(pm);
            bufferSize.decrementAndGet();
        }
        return batch;
    }

    private void safeInsert(List<PrivateMessage> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        try {
            dao.insertBatch(batch);
        } catch (Exception e) {
            //落库失败(如 Mongo 短暂不可用): 回填写缓冲, 下个周期重试; insertBatch 按 _id 幂等, 重投安全, 避免消息永久丢失。
            //回填不受容量上限约束(上限只挡新消息); 消息仍在会话索引中, 无需重建
            log.error("私聊批量落库失败, 回填重试 size={}", batch.size(), e);
            for (PrivateMessage pm : batch) {
                writeBuffer.offer(pm);
                bufferSize.incrementAndGet();
            }
            return;
        }
        //已落库: 从会话索引清除 (空桶移除, compute 系列保证与写入互斥)
        for (PrivateMessage pm : batch) {
            bufferIndex.computeIfPresent(pm.getConversationId(), (k, m) -> {
                m.remove(pm.getId());
                return m.isEmpty() ? null : m;
            });
        }
        //增量维护双侧会话摘要; 失败仅记日志: unread 在打开会话时会被清零校正, last* 由后续消息修正
        try {
            conversationDao.bulkApply(batch);
        } catch (Exception e) {
            log.error("会话摘要更新失败 size={}", batch.size(), e);
        }
    }

    /**
     * 分页拉取与某人的私聊历史 (打开会话时把发给我的标记已读)。
     * 拉首页(cursor=0)时会合并写缓冲中尚未落库的本会话消息, 避免刚发的消息查不到。
     */
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        String conversationId = PrivateMessage.conversationId(playerId, targetId);
        long cursorId = (cursor == null || cursor.isEmpty()) ? 0 : Long.parseLong(cursor);
        int size = SocialConst.Cfg.CHAT_PULL_SIZE;
        //本人对该会话的单向清除时间: 此前消息不展示 (投影只取清除标记, 不读全文档)
        long clearTime = friendDao.getConversationClear(playerId).getOrDefault(targetId, 0L);

        List<PrivateMessage> page = dao.page(conversationId, cursorId, clearTime, size);
        //仅打开会话(首页)时标记已读 + 合并写缓冲; 翻历史页时读态在打开时已处理, 不再重复 updateMulti
        if (cursorId == 0) {
            dao.markRead(conversationId, playerId);
            //摘要未读同步清零
            conversationDao.resetUnread(playerId, targetId);
            page = mergeBuffer(conversationId, playerId, clearTime, page, size);
        }

        Player me = corePlayerService.get(playerId);
        Player other = corePlayerService.get(targetId);

        List<ChatMessage> list = new ArrayList<>(page.size());
        //page 为最新在前, 反转为时间正序
        for (int i = page.size() - 1; i >= 0; i--) {
            PrivateMessage pm = page.get(i);
            Player from = pm.getFromId() == playerId ? me : (pm.getFromId() == targetId ? other : null);
            list.add(toChatMessage(pm, from));
        }

        //page 为 desc, 最后一条是最旧的一条, 作为下一页游标
        String nextCursor = null;
        if (page.size() >= size && !page.isEmpty()) {
            nextCursor = String.valueOf(page.get(page.size() - 1).getId());
        }
        return new ChatHistory(list, nextCursor);
    }

    /**
     * 合并写缓冲中本会话的消息到 DB 首页结果 (按 id 去重, 倒序, 截断到 size)。
     * 经会话索引只扫本会话桶, 不再全量遍历写缓冲。
     */
    private List<PrivateMessage> mergeBuffer(String conversationId, long playerId, long clearTime, List<PrivateMessage> page, int size) {
        Map<Long, PrivateMessage> bucket = bufferIndex.get(conversationId);
        if (bucket == null || bucket.isEmpty()) {
            return page;
        }
        List<PrivateMessage> buffered = new ArrayList<>();
        for (PrivateMessage pm : bucket.values()) {
            if (pm.getTime() > clearTime) {
                //发给我的同步标记已读(缓冲对象即落库对象)
                if (pm.getToId() == playerId) {
                    pm.setRead(true);
                }
                buffered.add(pm);
            }
        }
        if (buffered.isEmpty()) {
            return page;
        }
        Map<Long, PrivateMessage> map = new LinkedHashMap<>();
        for (PrivateMessage pm : page) {
            map.put(pm.getId(), pm);
        }
        for (PrivateMessage pm : buffered) {
            map.put(pm.getId(), pm);
        }
        List<PrivateMessage> merged = new ArrayList<>(map.values());
        merged.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        if (merged.size() > size) {
            merged = new ArrayList<>(merged.subList(0, size));
        }
        return merged;
    }

    /**
     * 会话列表 (与我有过私聊的对端, 按最新消息倒序)。
     * 读会话摘要表: 一次索引查询拿到最新消息与未读数, 不再扫消息表 + 聚合计数。
     */
    public List<ConversationInfo> conversationList(long playerId) {
        //只需要会话清除标记, 投影读取, 不拉全文档
        Map<Long, Long> clearMap = friendDao.getConversationClear(playerId);
        List<ConversationEntry> entries = conversationDao.listByPlayer(playerId, CONVERSATION_SCAN_LIMIT);

        //过滤掉已被本人清除且无更新消息的会话
        List<ConversationEntry> visible = new ArrayList<>();
        List<Long> targetIds = new ArrayList<>();
        for (ConversationEntry en : entries) {
            if (en.getLastTime() <= clearMap.getOrDefault(en.getTargetId(), 0L)) {
                continue;
            }
            visible.add(en);
            targetIds.add(en.getTargetId());
        }
        Map<Long, Player> players = corePlayerService.multiGetPlayerMap(targetIds);
        //一次 HMGET 批量判在线, 替代逐会话 online 的 N 次 Redis
        Map<Long, PlayerSessionInfo> sessionInfos = statusService.infosOf(targetIds);

        List<ConversationInfo> result = new ArrayList<>();
        for (ConversationEntry en : visible) {
            ConversationInfo info = new ConversationInfo();
            info.targetId = en.getTargetId();
            Player p = players.get(en.getTargetId());
            if (p != null) {
                info.nick = p.getNickName();
                info.headImg = p.getHeadImgId();
                info.headFrame = p.getHeadFrameId();
            }
            info.online = sessionInfos.containsKey(en.getTargetId());
            info.lastContent = en.getLastContent();
            info.lastTime = en.getLastTime();
            //单向删除时摘要未读已清零, 之后新消息重新累计, 无需按清除点特殊计数
            info.unread = en.getUnread();
            result.add(info);
        }
        return result;
    }

    /**
     * 删除与某人的会话 (单向): 仅记录本人侧清除时间, 不删除实际消息, 对方历史与未读不受影响;
     * 清除点之前的消息对本人不再展示, 之后再有新消息会重新出现。
     */
    public void deleteConversation(long playerId, long targetId) {
        long now = System.currentTimeMillis();
        //顺带清理早于消息保留期的过期清除标记, 防止 conversationClear 无界增长
        long expireBefore = now - TimeUnit.DAYS.toMillis(SocialConst.Cfg.PRIVATE_KEEP_DAYS);
        friendDao.setConversationClear(playerId, targetId, now, expireBefore);
        //清除点之前的未读不再展示, 摘要未读清零 (之后新消息会重新累计)
        conversationDao.resetUnread(playerId, targetId);
    }

    private ChatMessage toChatMessage(PrivateMessage pm, Player from) {
        ChatMessage m = new ChatMessage();
        m.setId(pm.getId());
        m.setChannel(ChatChannelType.PRIVATE.getCode());
        m.setFromId(pm.getFromId());
        m.setToId(pm.getToId());
        m.setContent(pm.getContent());
        m.setTime(pm.getTime());
        if (from != null) {
            m.setFromNick(from.getNickName());
            m.setFromHeadImg(from.getHeadImgId());
            m.setFromHeadFrame(from.getHeadFrameId());
        }
        return m;
    }
}
