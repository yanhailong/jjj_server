package com.jjg.game.hall.service;

import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.dao.NoticeDao;
import com.jjg.game.core.data.Notice;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/11/10 10:53
 */
@Service
public class NoticeService implements IRedDotService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NoticeDao noticeDao;
    @Autowired
    private RedDotManager redDotManager;

    private volatile List<Notice> notices = List.of();

    public void init() {
        loadNotice(true);
    }

    /**
     * 加载公告列表
     */
    public void loadNotice(boolean init) {
        int now = TimeHelper.nowInt();

        List<Notice> all = noticeDao.getNoticeList(now).stream()
                .sorted(Comparator.comparingInt(Notice::getSort))
                .collect(Collectors.toList());

        // 找出新增的公告
        boolean newNotices = false;
        if (!init) {
            newNotices = all.stream().anyMatch(notice -> notices.stream()
                    .noneMatch(existing -> existing.getId() == notice.getId()));
        }

        // 更新notices列表为最新的
        this.notices = all;
        log.info("加载公告列表成功，count = {},newNotices = {}", notices.size(), newNotices);

        //如果有新增的公告，则要发送红点通知给玩家
        if (newNotices) {
            // 公告红点与玩家已读记录有关，不能广播固定数量，必须逐个玩家重新计算。
            redDotManager.updateRedDotByInitializeForOnlinePlayers(RedDotDetails.RedDotModule.NOTICE, 0);
        }
    }

    /**
     * 获取缓存的公告列表
     *
     * @return
     */
    public List<Notice> getNotices() {
        int now = TimeHelper.nowInt();

        // 返回当前有效公告快照，避免逐玩家并发刷新红点时修改共享列表。
        return this.notices.stream()
                .filter(notice -> notice.isOpen() && notice.getStartTime() <= now && notice.getEndTime() >= now)
                .toList();
    }

    public Set<Long> getPlayerReadNotice(long playerId) {
        return this.noticeDao.getPlayerReadNotice(playerId);
    }

    /**
     * 阅读邮件
     *
     * @param playerId
     * @param noticeId
     */
    public void readNotice(long playerId, long noticeId) {
        boolean match = this.notices.stream().anyMatch(notice -> notice.getId() == noticeId);
        if (match) {
            noticeDao.readNotice(playerId, noticeId);
            redDotManager.updateRedDotByInitialize(getModule(), getSubmodule(), playerId);
        }
    }


    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.NOTICE;
    }

    @Override
    public Set<NodeType> getSupportedNodeTypes() {
        return Set.of(NodeType.HALL, NodeType.GAME);
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        Set<Long> set = noticeDao.getPlayerReadNotice(playerId);
        RedDotDetails redDotDetailInfo = new RedDotDetails();
        redDotDetailInfo.setRedDotModule(RedDotDetails.RedDotModule.NOTICE);
        redDotDetailInfo.setRedDotType(RedDotDetails.RedDotType.COUNT);
        // 公告入口展示全部未读数量，普通公告和活动公告都参与统计。
        long unreadCount = getNotices().stream()
                .filter(notice -> !set.contains(notice.getId()))
                .count();
        redDotDetailInfo.setCount(unreadCount);
        return List.of(redDotDetailInfo);
    }

    public void removeReadData(long playerId) {
        noticeDao.removeReadData(playerId);
    }
}
