package com.jjg.game.core.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.MarqueeDao;
import com.jjg.game.core.data.LanguageData;
import com.jjg.game.core.data.LanguageParamData;
import com.jjg.game.core.data.Marquee;
import com.jjg.game.core.pb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/8/13 14:20
 */
@Component
public class CoreMarqueeManager implements TimerListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarqueeDao marqueeDao;
    @Autowired
    private TimerCenter timerCenter;
    @Autowired
    private MarsCurator marsCurator;
    @Autowired
    private ClusterSystem clusterSystem;

    //排序后的跑马灯列表,  在列表越靠后，越优先
    private LinkedList<Marquee> sortedMarquees;
    //排序后的玩家中奖跑马灯列表,  在列表越靠后，越优先
    private LinkedList<Marquee> playerWinSortedMarquees;
    //排序后的活动跑马灯列表,  在列表越靠后，越优先
    private LinkedList<Marquee> activitySortedMarquees;
    //映射表
    private Map<Integer, Marquee> marqueeMap;
    //当前正在运行的跑马灯
    private volatile int nowRunMarqueeId;
    //后台跑马灯下一次触发时间
    private final Map<Integer, Integer> marqueeNextPlayTimeMap = new ConcurrentHashMap<>();
    //修改跑马灯添加的锁
    private final Object lock = new Object();
    //当前跑马灯预计结束时间
    private volatile int nowRunMarqueeEndTime;

    //每个队列中保存的跑马灯最大个数
    private final int MAX_MARQUEE_COUNT = 50;

    private TimerEvent<String> checkEvent;

    public void init() {
        loadAllmarquee();
        addCheckEvent();
    }

    private void addCheckEvent() {
        this.checkEvent = new TimerEvent<>(this, "checkEvent", 1).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(this.checkEvent);
    }

    /**
     * 裁剪跑马灯队列
     *
     * @param list
     * @param maxCount
     * @return
     */
    private LinkedList<Marquee> keepTopNFromTail(LinkedList<Marquee> list, int maxCount) {
        if (list == null || list.size() <= maxCount) {
            return list;
        }

        int splitIndex = list.size() - maxCount;
        List<Marquee> removeList = new ArrayList<>(list.subList(0, splitIndex));
        List<Integer> removeIds = new ArrayList<>(removeList.size());
        for (Marquee marquee : removeList) {
            if (this.marqueeMap != null) {
                this.marqueeMap.remove(marquee.getId());
            }
            clean(marquee.getId());
            removeIds.add(marquee.getId());
        }
        removeFromRedisBatch(removeIds);
        return new LinkedList<>(list.subList(splitIndex, list.size()));
    }

    /**
     * 加载所有的跑马灯信息
     */
    private void loadAllmarquee() {
        List allMarquee = marqueeDao.getAllMarquee();
        if (allMarquee == null || allMarquee.isEmpty()) {
            return;
        }

        Map<Integer, Marquee> tmpMarqueeMap = new ConcurrentHashMap<>();
        List<Marquee> tmpList = new ArrayList<>();
        List<Marquee> tmpPlayerWinList = new ArrayList<>();
        List<Marquee> tmpActivityList = new ArrayList<>();
        for (Object o : allMarquee) {
            Marquee marquee = (Marquee) o;
            switch (marquee.getType()) {
                case GameConstant.Marquee.PLAYER_WIN -> tmpPlayerWinList.add(marquee);
                case GameConstant.Marquee.ACTIVITY -> tmpActivityList.add(marquee);
                default -> tmpList.add(marquee);
            }
            tmpMarqueeMap.put(marquee.getId(), marquee);
        }

        this.sortedMarquees = sortMarquee(tmpList);
        this.playerWinSortedMarquees = keepTopNFromTail(sortMarqueeByCreateTime(tmpPlayerWinList), MAX_MARQUEE_COUNT);
        this.activitySortedMarquees = keepTopNFromTail(sortMarqueeByCreateTime(tmpActivityList), MAX_MARQUEE_COUNT);
        this.marqueeMap = tmpMarqueeMap;

        log.debug("初始加载跑马灯后打印 map.size = {}", this.marqueeMap.size());
    }

    /**
     * 添加信息的跑马灯后要进行排序
     *
     * @param marquee
     */
    public void addNewMarquee(Marquee marquee) {
        if (this.marqueeMap == null) {
            this.marqueeMap = new ConcurrentHashMap<>();
        }

        this.marqueeMap.remove(marquee.getId());
        initNextPlayTime(marquee);

        switch (marquee.getType()) {
            case GameConstant.Marquee.PLAYER_WIN -> {
                List<Marquee> marqueeList;
                if (this.playerWinSortedMarquees != null) {
                    marqueeList = new ArrayList<>(this.playerWinSortedMarquees);
                } else {
                    marqueeList = new ArrayList<>();
                }
                marqueeList.add(marquee);
                this.playerWinSortedMarquees = keepTopNFromTail(sortMarqueeByCreateTime(marqueeList), MAX_MARQUEE_COUNT);
            }
            case GameConstant.Marquee.ACTIVITY -> {
                List<Marquee> marqueeList;
                if (this.activitySortedMarquees != null) {
                    marqueeList = new ArrayList<>(this.activitySortedMarquees);
                } else {
                    marqueeList = new ArrayList<>();
                }
                marqueeList.add(marquee);
                this.activitySortedMarquees = keepTopNFromTail(sortMarqueeByCreateTime(marqueeList), MAX_MARQUEE_COUNT);
            }
            default -> {
                List<Marquee> marqueeList;
                if (this.sortedMarquees != null) {
                    marqueeList = new ArrayList<>(this.sortedMarquees);
                } else {
                    marqueeList = new ArrayList<>();
                }
                marqueeList.add(marquee);
                this.sortedMarquees = sortMarquee(marqueeList);
            }
        }

        this.marqueeMap.put(marquee.getId(), marquee);

        log.debug("添加跑马灯后打印 map.size = {}", this.marqueeMap.size());
        check();
    }

    /**
     * 删除跑马灯后要进行排序
     *
     * @param id
     */
    public void removeMarquee(int id) {
        //保证该id的跑马灯一定会被删除
        Marquee remove = this.marqueeMap.remove(id);
        this.marqueeNextPlayTimeMap.remove(id);
        removeFromRedis(id);
        addNotifyStopEvent(id);
        clean(id);

        if (remove == null) {
            return;
        }

        switch (remove.getType()) {
            case GameConstant.Marquee.PLAYER_WIN -> {
                if (this.playerWinSortedMarquees == null || this.playerWinSortedMarquees.isEmpty()) {
                    return;
                }
                List<Marquee> marqueeList = new ArrayList<>(this.playerWinSortedMarquees);
                marqueeList.removeIf(marquee -> marquee.getId() == id);
                this.playerWinSortedMarquees = keepTopNFromTail(sortMarqueeByCreateTime(marqueeList), MAX_MARQUEE_COUNT);
            }
            case GameConstant.Marquee.ACTIVITY -> {
                if (this.activitySortedMarquees == null || this.activitySortedMarquees.isEmpty()) {
                    return;
                }
                List<Marquee> marqueeList = new ArrayList<>(this.activitySortedMarquees);
                marqueeList.removeIf(marquee -> marquee.getId() == id);
                this.activitySortedMarquees = keepTopNFromTail(sortMarqueeByCreateTime(marqueeList), MAX_MARQUEE_COUNT);
            }
            default -> {
                if (this.sortedMarquees == null || this.sortedMarquees.isEmpty()) {
                    return;
                }
                List<Marquee> marqueeList = new ArrayList<>(this.sortedMarquees);
                marqueeList.removeIf(marquee -> marquee.getId() == id);
                this.sortedMarquees = sortMarquee(marqueeList);
            }
        }

        log.debug("删除跑马灯后打印 map.size = {}", this.marqueeMap.size());
    }

    /**
     * 删除跑马灯
     *
     * @param id
     */
    private void removeFromRedis(int id) {
        //防止在节点变更时，遗漏删除中奖的跑马灯，所以只要是主节点都可以进行删除
        if (marsCurator.isMaster()) {
            marqueeDao.removeMarquee(id);
            log.debug("从redis删除跑马灯 id = {}", id);
        }
    }

    /**
     * 进行排序
     *
     * @param list
     * @return
     */
    private LinkedList<Marquee> sortMarquee(List<Marquee> list) {
        //进行排序
        return list
                .stream()
                .sorted(Comparator
                        .comparingInt(com.jjg.game.core.data.Marquee::getType).reversed()  // 先按 type 降序
                        .thenComparingInt(com.jjg.game.core.data.Marquee::getPriority)     // 再按 priority 升序
                )
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * 进行排序
     *
     * @param list
     * @return
     */
    private LinkedList<Marquee> sortMarqueeByCreateTime(List<Marquee> list) {
        //进行排序
        return list
                .stream()
                .sorted(Comparator
                        .comparingInt(com.jjg.game.core.data.Marquee::getCreateTime).reversed()  // 按 createTime 降序
                )
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * 检查跑马灯列表
     */
    public void check() {
        int now = TimeHelper.nowInt();
        finishCurrentIfNeed(now);
        if (isRunning(now)) {
            return;
        }

        // 先评估后台紧急跑马灯：如果当前就该播，立即接管；否则根据下一次触发时间决定是否允许低优先级穿插。
        BackendSchedule backendSchedule = inspectBackendMarquees(now);
        if (backendSchedule.readyMarquee() != null) {
            playMarquee(backendSchedule.readyMarquee(), now);
            return;
        }
        if (backendSchedule.blockOtherTypes()) {
            return;
        }

        Marquee playerWinMarquee = findPendingMarquee(this.playerWinSortedMarquees, now, backendSchedule.nextPlayTime(), "移除过期中奖跑马灯 id = {}");
        if (playerWinMarquee != null) {
            playMarquee(playerWinMarquee, now);
            return;
        }

        Marquee activityMarquee = findPendingMarquee(this.activitySortedMarquees, now, backendSchedule.nextPlayTime(), "移除过期活动跑马灯 id = {}");
        if (activityMarquee != null) {
            playMarquee(activityMarquee, now);
        }
    }

    @Override
    public void onTimer(TimerEvent e) {
        if (e == this.checkEvent) {
            check();
        } else {
            String[] arr = e.getParameter().toString().split("_");
            int id = Integer.parseInt(arr[1]);
            if ("notifyStopEvent".equals(arr[0])) {
                notifyClientStopMarquee(id);
            }
        }
    }

    /**
     * 通知客户端停止跑马灯
     *
     * @param id
     */
    private void addNotifyStopEvent(int id) {
        TimerEvent<String> nodeEvent = new TimerEvent<>(this, 1, "notifyStopEvent_" + id).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(nodeEvent);
        clean(id);
        log.debug("添加通知客户端停止跑马灯事件 id = {}", id);
    }

    /**
     * 通知所有的大厅和游戏节点开始跑马灯
     */
    public void notifyHallAndGameNodeStartMarquee(NotifyAllNodesMarqueeServer notify) {
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        clusterSystem.notifyHallAndGameNode(pfMessage);
    }

    /**
     * 通知所有的大厅和游戏节点开始跑马灯
     */
    public void notifyHallAndGameNodeStopMarquee(NotifyAllNodesStopMarqueeServer notify) {
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        clusterSystem.notifyHallAndGameNode(pfMessage);
    }


    /**
     * 通知当前节点，所有的客户端要展示的跑马灯
     *
     * @param marquee
     */
    private void notifyClientMarquee(Marquee marquee) {
        NotifyMarquee notify = new NotifyMarquee();
        notify.marqueeInfo = transMarqueeInfo(marquee);
        log.debug("通知客户端跑马灯 marquee = {}", JSON.toJSONString(notify));
        // 广播消息
        clusterSystem.broadcastToOnlinePlayer(notify);
    }

    /**
     * 通知当前节点，所有的客户端要停止的跑马灯
     *
     * @param id
     */
    private void notifyClientStopMarquee(int id) {
        NotifyStopMarquee notify = new NotifyStopMarquee();
        notify.id = id;
        // 广播消息
        clusterSystem.broadcastToOnlinePlayer(notify);
    }

    /**
     * 获取当前正在运行的跑马灯
     *
     * @return
     */
    public Marquee getCurrentMarquee() {
        if (this.marqueeMap == null || this.marqueeMap.isEmpty()) {
            return null;
        }
        return this.marqueeMap.get(this.nowRunMarqueeId);
    }

    public int getClientShowGarqueeType(int marqueeType) {
        if (marqueeType == GameConstant.Marquee.PLAYER_WIN) {
            return GameConstant.Marquee.CLIENT_LANG_TYPE;
        }
        return GameConstant.Marquee.CLIENT_NORMAL_TYPE;
    }

    /**
     * 玩家中奖的跑马灯
     *
     * @param playerNickName 玩家昵称
     * @param langId         跑马灯内容的多语言id
     * @param gameLangId     游戏名称的多语言id
     * @param value          金额
     */
    public void playerWinMarquee(String playerNickName, int langId, int gameLangId, long value) {

        log.debug("添加玩家中奖的跑马灯 nick = {},langId = {},gameLangId = {},value = {}", playerNickName, langId, gameLangId, value);

        Marquee marquee = new Marquee();

        marquee.setType(GameConstant.Marquee.PLAYER_WIN);
        marquee.setShowTime(GameConstant.Marquee.PLAYER_WIN_INTERVAL);
        marquee.setInterval(GameConstant.Marquee.PLAYER_WIN_INTERVAL);
        marquee.setCreateTime(TimeHelper.nowInt());

        LanguageData contentData = new LanguageData();
        contentData.setLangId(langId);
        contentData.setType(GameConstant.Language.TYPE_LANGUAGE_MATCH);

        List<LanguageParamData> params = new ArrayList<>();
        addMarqueeParam(params, GameConstant.Marquee.CLIENT_NORMAL_TYPE, playerNickName);
        addMarqueeParam(params, GameConstant.Marquee.CLIENT_LANG_TYPE, gameLangId + "");
        addMarqueeParam(params, GameConstant.Marquee.CLIENT_NORMAL_TYPE, value + "");
        contentData.setParams(params);

        marquee.setContent(contentData);

        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            marquee.setId(RandomUtils.randomNum(-999999, -1));
            //添加到redis
            boolean add = marqueeDao.addMarqueeIfAbsent(marquee);
            if (add) {
                //通知其他服务器
                //构建请求消息
                NotifyAllNodesMarqueeServer notify = new NotifyAllNodesMarqueeServer();
                notify.marqueeInfo = transMarqueeInfo(marquee);
                notify.type = marquee.getType();
                notifyHallAndGameNodeStartMarquee(notify);
                addNewMarquee(marquee);
                break;
            }
        }
    }

    /**
     * 活动开启跑马灯
     *
     * @param langId 跑马灯内容的多语言id
     */
    public void activityMarquee(int langId) {
        if (langId == 0) {
            return;
        }
        log.debug("活动开启的跑马灯 langId = {}", langId);
        Marquee marquee = new Marquee();

        marquee.setType(GameConstant.Marquee.ACTIVITY);
        marquee.setShowTime(GameConstant.Marquee.ACTIVITY_INTERVAL);
        marquee.setInterval(GameConstant.Marquee.ACTIVITY_INTERVAL);
        marquee.setCreateTime(TimeHelper.nowInt());

        LanguageData contentData = new LanguageData();
        contentData.setLangId(langId);
        contentData.setType(GameConstant.Language.TYPE_LANGUAGE_MATCH);
        marquee.setContent(contentData);

        for (int i = 0; i < CoreConst.Common.REDIS_TRY_COUNT; i++) {
            marquee.setId(RandomUtils.randomNum(-999999, -1));
            //添加到redis
            boolean add = marqueeDao.addMarqueeIfAbsent(marquee);
            if (add) {
                //构建请求消息
                NotifyAllNodesMarqueeServer notify = new NotifyAllNodesMarqueeServer();
                notify.marqueeInfo = transMarqueeInfo(marquee);
                notify.type = marquee.getType();
                notifyHallAndGameNodeStartMarquee(notify);
                addNewMarquee(marquee);
                break;
            }
        }
    }

    /**
     * 添加多语言参数
     *
     * @param params
     * @param type
     * @param param
     */
    private void addMarqueeParam(List<LanguageParamData> params, int type, String param) {
        LanguageParamData lang = new LanguageParamData();
        lang.setType(type);
        lang.setParam(param);
        params.add(lang);
    }

    /**
     * 将跑马灯对象转化为 协议结构体
     *
     * @param marquee
     * @return
     */
    public MarqueeInfo transMarqueeInfo(Marquee marquee) {
        MarqueeInfo marqueeInfo = new MarqueeInfo();
        marqueeInfo.id = marquee.getId();
        marqueeInfo.interval = marquee.getInterval();
        marqueeInfo.startTime = marquee.getStartTime();
        marqueeInfo.endTime = marquee.getEndTime();
        marqueeInfo.showTime = marquee.getShowTime();

        marqueeInfo.content = marquee.getContent().toPbInfo();
        marqueeInfo.content.type = getClientShowGarqueeType(marquee.getType());
        return marqueeInfo;
    }

    /**
     * 批量删除跑马灯
     *
     * @param ids
     */
    private void removeFromRedisBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (marsCurator.isMaster()) {
            marqueeDao.removeMarquees(ids);
            log.debug("批量删除跑马灯 = {}", ids);
        }
    }

    private void clean(int id) {
        synchronized (lock) {
            if (nowRunMarqueeId == id) {
                nowRunMarqueeId = 0;
                nowRunMarqueeEndTime = 0;
            }
        }
        this.marqueeNextPlayTimeMap.remove(id);
    }

    private boolean update(int id, int endTime) {
        synchronized (lock) {
            if (nowRunMarqueeId == 0) {
                nowRunMarqueeId = id;
                nowRunMarqueeEndTime = endTime;
                return true;
            }
        }
        return false;
    }

    private void initNextPlayTime(Marquee marquee) {
        if (isBackendMarquee(marquee)) {
            this.marqueeNextPlayTimeMap.put(marquee.getId(), marquee.getStartTime());
        }
    }

    private boolean isBackendMarquee(Marquee marquee) {
        return marquee.getType() != GameConstant.Marquee.PLAYER_WIN
                && marquee.getType() != GameConstant.Marquee.ACTIVITY;
    }

    private int getPlayDuration(Marquee marquee) {
        return Math.max(marquee.getShowTime(), 1);
    }

    private int getCycleDuration(Marquee marquee) {
        return getPlayDuration(marquee) + Math.max(marquee.getInterval(), 0);
    }

    private int getNextPlayTime(Marquee marquee) {
        return this.marqueeNextPlayTimeMap.getOrDefault(marquee.getId(), marquee.getStartTime());
    }

    private void finishCurrentIfNeed(int now) {
        synchronized (lock) {
            if (nowRunMarqueeId != 0 && now >= nowRunMarqueeEndTime) {
                nowRunMarqueeId = 0;
                nowRunMarqueeEndTime = 0;
            }
        }
    }

    private boolean isRunning(int now) {
        synchronized (lock) {
            return nowRunMarqueeId != 0 && now < nowRunMarqueeEndTime;
        }
    }

    private Marquee findPendingMarquee(LinkedList<Marquee> marquees, int now, int nextBackendPlayTime, String logText) {
        if (marquees == null || marquees.isEmpty()) {
            return null;
        }

        ListIterator<Marquee> it = marquees.listIterator(marquees.size());
        while (it.hasPrevious()) {
            Marquee marquee = it.previous();
            if (marquee.getStartTime() < 1) {
                if (canFitBeforeBackend(marquee, now, nextBackendPlayTime)) {
                    return marquee;
                }
                continue;
            }

            if (now > marquee.getEndTime()) {
                removeExpiredMarquee(it, marquee, logText);
            }
        }
        return null;
    }

    private void removeExpiredMarquee(ListIterator<Marquee> it, Marquee marquee, String logText) {
        it.remove();
        this.marqueeMap.remove(marquee.getId());
        removeFromRedis(marquee.getId());
        clean(marquee.getId());
        log.debug(logText, marquee.getId());
    }

    private boolean isExclusiveBackend(Marquee marquee) {
        return marquee.getInterval() < 60;
    }

    private boolean canFitBeforeBackend(Marquee marquee, int now, int nextBackendPlayTime) {
        if (nextBackendPlayTime < 1) {
            return true;
        }
        // 后台下一轮开始前剩余时间不够完整播完一条低优先级跑马灯时，不再插播。
        return now + getPlayDuration(marquee) <= nextBackendPlayTime;
    }

    private void playMarquee(Marquee marquee, int now) {
        int endTime = now + getPlayDuration(marquee);
        if (!update(marquee.getId(), endTime)) {
            return;
        }

        if (isBackendMarquee(marquee)) {
            this.marqueeNextPlayTimeMap.put(marquee.getId(), now + getCycleDuration(marquee));
        } else if (marquee.getStartTime() < 1) {
            marquee.setStartTime(now);
            marquee.setEndTime(endTime);
        }

        notifyClientMarquee(marquee);
    }

    private BackendSchedule inspectBackendMarquees(int now) {
        if (this.sortedMarquees == null || this.sortedMarquees.isEmpty()) {
            return BackendSchedule.EMPTY;
        }

        int nextPlayTime = 0;
        boolean blockOtherTypes = false;
        ListIterator<Marquee> it = this.sortedMarquees.listIterator(this.sortedMarquees.size());
        while (it.hasPrevious()) {
            Marquee marquee = it.previous();
            if (now > marquee.getEndTime()) {
                removeExpiredMarquee(it, marquee, "移除过期跑马灯 id = {}");
                continue;
            }

            if (now < marquee.getStartTime()) {
                continue;
            }

            int marqueeNextPlayTime = getNextPlayTime(marquee);
            if (marqueeNextPlayTime <= now) {
                // 排序靠后的后台跑马灯优先级更高，命中后直接播放。
                return new BackendSchedule(marquee, nextPlayTime, blockOtherTypes);
            }

            nextPlayTime = nextPlayTime == 0 ? marqueeNextPlayTime : Math.min(nextPlayTime, marqueeNextPlayTime);
            blockOtherTypes |= isExclusiveBackend(marquee);
        }
        return new BackendSchedule(null, nextPlayTime, blockOtherTypes);
    }

    /**
     * 后台跑马灯调度快照。
     *
     * @param readyMarquee    当前应立即播放的后台跑马灯
     * @param nextPlayTime    当前所有有效后台跑马灯中最早的下一次触发时间
     * @param blockOtherTypes 存在短间隔后台跑马灯时，在等待窗口内也不允许其他类型穿插
     */
    private record BackendSchedule(Marquee readyMarquee, int nextPlayTime, boolean blockOtherTypes) {
        private static final BackendSchedule EMPTY = new BackendSchedule(null, 0, false);
    }
}
