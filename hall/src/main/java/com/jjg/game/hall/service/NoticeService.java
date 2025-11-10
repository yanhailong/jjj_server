package com.jjg.game.hall.service;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
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

    private List<Notice> notices = new ArrayList<>();

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
            Supplier<List<RedDotDetails>> supplier = () -> {
                RedDotDetails redDotDetailInfo = new RedDotDetails();
                redDotDetailInfo.setRedDotModule(RedDotDetails.RedDotModule.NOTICE);
                redDotDetailInfo.setRedDotType(RedDotDetails.RedDotType.COMMON);
                redDotDetailInfo.setCount(1);
                return List.of(redDotDetailInfo);
            };

            redDotManager.updateRedDot(supplier, 0);
        }
    }

    /**
     * 获取缓存的公告列表
     *
     * @return
     */
    public List<Notice> getNotices() {
        int now = TimeHelper.nowInt();

        //移除未开启的公告
        this.notices.removeIf(notice -> !notice.isOpen() || notice.getStartTime() > now || notice.getEndTime() < now);
        return this.notices;
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
        boolean match = this.notices.stream().anyMatch(notice -> {
            return notice.getId() == noticeId;
        });
        if (match) {
            noticeDao.readNotice(playerId, noticeId);
        }
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.NOTICE;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        Set<Long> set = noticeDao.getPlayerReadNotice(playerId);

        boolean match = this.notices.stream().anyMatch(notice -> !set.contains(notice.getId()));
        if (!match) {
            return List.of();
        }
        RedDotDetails redDotDetailInfo = new RedDotDetails();
        redDotDetailInfo.setRedDotModule(RedDotDetails.RedDotModule.NOTICE);
        redDotDetailInfo.setRedDotType(RedDotDetails.RedDotType.COMMON);
        redDotDetailInfo.setCount(1);
        return List.of(redDotDetailInfo);
    }

    public void removeReadData(long playerId){
        noticeDao.removeReadData(playerId);
    }
}