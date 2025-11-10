package com.jjg.game.hall.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.dao.NoticeDao;
import com.jjg.game.core.data.Notice;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/11/10 10:53
 */
@Service
public class NoticeService implements IPlayerLoginSuccess {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NoticeDao noticeDao;

    private List<Notice> notices = new ArrayList<>();

    public void init() {
        loadNotice();
    }

    /**
     * 加载公告列表
     */
    public void loadNotice() {
        int now = TimeHelper.nowInt();

        List<Notice> all = noticeDao.getNoticeList(now).stream()
                .sorted(Comparator.comparingInt(Notice::getSort))
                .collect(Collectors.toList());

        // 找出新增的公告
        List<Notice> newNotices = all.stream()
                .filter(notice -> notices.stream()
                        .noneMatch(existing -> existing.getId() == notice.getId()))
                .collect(Collectors.toList());

        //如果有新增的公告，则要发送红点通知给玩家
        if (!newNotices.isEmpty()) {

        }

        // 更新notices列表为最新的
        this.notices = all;
        log.info("加载公告列表成功，count = {},newNotices.size = {}", notices.size(), newNotices.size());
    }

    /**
     * 获取缓存的公告列表
     * @return
     */
    public List<Notice> getNotices() {
        int now = TimeHelper.nowInt();

        //移除未开启的公告
        this.notices.removeIf(notice -> !notice.isOpen() || notice.getStartTime() > now || notice.getEndTime() < now);
        return this.notices;
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, boolean firstLogin) {

    }
}