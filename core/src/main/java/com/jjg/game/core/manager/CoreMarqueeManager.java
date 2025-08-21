package com.jjg.game.core.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
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
    //映射表
    private Map<Integer, Marquee> marqueeMap;
    //当前正在运行的跑马灯
    private int nowRunMarqueeId;

    private TimerEvent<String> checkEvent;

    public void init() {
        loadAllmarquee();
        addCheckEvent();
    }


    private void addCheckEvent() {
        this.checkEvent = new TimerEvent<>(this, "checkEvent", 3).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(this.checkEvent);
    }

    /**
     * 加载所有的跑马灯信息
     */
    private void loadAllmarquee() {
        List allMarquee = marqueeDao.getAllMarquee();
        if (allMarquee == null || allMarquee.isEmpty()) {
            return;
        }

        Map<Integer, Marquee> tmpMarqueeMap = new HashMap<>();
        List<Marquee> tmpList = new ArrayList<>();
        List<Marquee> tmpPlayerWinList = new ArrayList<>();
        for (Object o : allMarquee) {
            Marquee marquee = (Marquee) o;
            if (marquee.getType() == GameConstant.Marquee.PLAYER_WIN) {
                tmpPlayerWinList.add(marquee);
            } else {
                tmpList.add(marquee);
            }
            tmpMarqueeMap.put(marquee.getId(), marquee);
        }

        this.sortedMarquees = sortMarquee(tmpList);
        this.playerWinSortedMarquees = sortPlayerWinMarquee(tmpPlayerWinList);
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
            this.marqueeMap = new HashMap<>();
        }
        if (this.marqueeMap.containsKey(marquee.getId())) {
            this.marqueeMap.put(marquee.getId(), marquee);
            return;
        }

        if (marquee.getType() == GameConstant.Marquee.PLAYER_WIN) {
            List<Marquee> marqueeList = null;
            if (this.playerWinSortedMarquees != null) {
                marqueeList = new ArrayList<>(this.playerWinSortedMarquees);
            } else {
                marqueeList = new ArrayList<>();
            }
            marqueeList.add(marquee);
            this.playerWinSortedMarquees = sortPlayerWinMarquee(marqueeList);
        } else {
            List<Marquee> marqueeList = null;
            if (this.sortedMarquees != null) {
                marqueeList = new ArrayList<>(this.sortedMarquees);
            } else {
                marqueeList = new ArrayList<>();
            }
            marqueeList.add(marquee);
            this.sortedMarquees = sortMarquee(marqueeList);
        }
        this.marqueeMap.put(marquee.getId(), marquee);

        log.debug("添加跑马灯后打印 map.size = {}", this.marqueeMap.size());
    }

    /**
     * 删除跑马灯后要进行排序
     *
     * @param id
     */
    public void removeMarquee(int id) {
        Marquee remove = this.marqueeMap.remove(id);
        if (remove == null) {
            return;
        }
        removeFromeRedis(id);
        addNotifyStopEvent(id);

        if (this.nowRunMarqueeId == id) {
            this.nowRunMarqueeId = 0;
        }

        if (remove.getType() == GameConstant.Marquee.PLAYER_WIN) {
            if (this.playerWinSortedMarquees == null || this.playerWinSortedMarquees.isEmpty()) {
                return;
            }
            List<Marquee> marqueeList = new ArrayList<>(this.playerWinSortedMarquees);
            marqueeList.removeIf(marquee -> marquee.getId() == id);
            this.playerWinSortedMarquees = sortPlayerWinMarquee(marqueeList);
        } else {
            if (this.sortedMarquees == null || this.sortedMarquees.isEmpty()) {
                return;
            }
            List<Marquee> marqueeList = new ArrayList<>(this.sortedMarquees);
            marqueeList.removeIf(marquee -> marquee.getId() == id);
            this.sortedMarquees = sortMarquee(marqueeList);
        }

        log.debug("删除跑马灯后打印 map.size = {}", this.marqueeMap.size());
        for (Marquee m : this.sortedMarquees) {
            System.out.println(JSON.toJSONString(m));
        }
    }

    /**
     * 删除跑马灯
     *
     * @param id
     */
    private void removeFromeRedis(int id) {
        //必须是大厅主节点，才能从redis删除
        if (marsCurator.isMaster() && clusterSystem.nodeConfig.getType().equalsIgnoreCase(NodeType.HALL.name())) {
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
    private LinkedList<Marquee> sortPlayerWinMarquee(List<Marquee> list) {
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
        //标记是否找到有效的跑马灯
        boolean findMarquee = false;

        //先检查优先级高的跑马灯
        if (this.sortedMarquees != null && !this.sortedMarquees.isEmpty()) {
            // 获取从尾部开始的 ListIterator
            ListIterator<Marquee> it = this.sortedMarquees.listIterator(this.sortedMarquees.size());
            while (it.hasPrevious()) {
                Marquee marquee = it.previous();
                if (now > marquee.getEndTime()) {
                    //删除过期的跑马灯
                    it.remove();
                    this.marqueeMap.remove(marquee.getId());
                    removeFromeRedis(marquee.getId());
                    if (this.nowRunMarqueeId == marquee.getId()) {
                        this.nowRunMarqueeId = 0;
                    }
                    log.debug("移除过期跑马灯 id = {}", marquee.getId());
                    continue;
                }

                if (now < marquee.getStartTime()) {
                    continue;
                }

                if (this.nowRunMarqueeId != marquee.getId()) {
                    this.nowRunMarqueeId = marquee.getId();
                    addNotifySendEvent(marquee.getId());
                }
                findMarquee = true;
                break;
            }
        }

        //再检查玩家中奖的跑马灯
        if (!findMarquee && this.playerWinSortedMarquees != null && !this.playerWinSortedMarquees.isEmpty()) {
            // 获取从尾部开始的 ListIterator
            ListIterator<Marquee> it = this.playerWinSortedMarquees.listIterator(this.playerWinSortedMarquees.size());
            while (it.hasPrevious()) {
                Marquee marquee = it.previous();
                if (marquee.getStartTime() < 1) {  //表示这条中奖的跑马灯还没有推送
                    this.nowRunMarqueeId = marquee.getId();
                    addNotifySendEvent(marquee.getId());
                    break;
                } else {
                    if (now > marquee.getEndTime()) {
                        //删除过期的跑马灯
                        it.remove();
                        this.marqueeMap.remove(marquee.getId());
                        removeFromeRedis(marquee.getId());
                        if (this.nowRunMarqueeId == marquee.getId()) {
                            this.nowRunMarqueeId = 0;
                        }
                        log.debug("移除过期中奖跑马灯 id = {}", marquee.getId());
                        continue;
                    }

                    if (now < marquee.getStartTime()) {
                        continue;
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onTimer(TimerEvent e) {
        if (e == this.checkEvent) {
            check();
        } else {
            String[] arr = e.getParameter().toString().split("_");
            int id = Integer.parseInt(arr[1]);

            if ("notifySendEvent".equals(arr[0])) {
                Marquee marquee = this.marqueeMap.get(id);
                if (marquee == null) {
                    return;
                }
                notifyClientMarquee(marquee);
            } else if ("notifyStopEvent".equals(arr[0])) {
                notifyClientStopMarquee(id);
            }
        }
    }

    /**
     * 通知客户端开始跑马灯
     *
     * @param id
     */
    private void addNotifySendEvent(long id) {
        TimerEvent<String> nodeEvent = new TimerEvent<>(this, 1, "notifySendEvent_" + id).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(nodeEvent);
    }

    /**
     * 通知客户端停止跑马灯
     *
     * @param id
     */
    private void addNotifyStopEvent(long id) {
        TimerEvent<String> nodeEvent = new TimerEvent<>(this, 1, "notifyStopEvent_" + id).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(nodeEvent);
        if (this.nowRunMarqueeId == id) {
            this.nowRunMarqueeId = 0;
        }
        log.debug("添加通知客户端停止跑马灯事件 id = {}", id);
    }

    /**
     * 通知所有的大厅和游戏节点开始跑马灯
     */
    public void notifyHallAndGameNodeStartMarquee(NotifyAllNodesMarqueeServer notify) {
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        notifyHallAndGameNode(pfMessage);
    }

    /**
     * 通知所有的大厅和游戏节点开始跑马灯
     */
    public void notifyHallAndGameNodeStopMarquee(NotifyAllNodesStopMarqueeServer notify) {
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        notifyHallAndGameNode(pfMessage);
    }

    /**
     * 通知所有的大厅和游戏节点
     */
    private void notifyHallAndGameNode(PFMessage pfMessage) {
        ClusterMessage msg = new ClusterMessage(pfMessage);

        //获取大厅节点
        List<ClusterClient> hallNodes = ClusterSystem.system.getNodesByType(NodeType.HALL);
        //获取游戏节点
        List<ClusterClient> gameNodes = ClusterSystem.system.getNodesByType(NodeType.GAME);

        //通知大厅节点
        for (ClusterClient clusterClient : hallNodes) {
            try {
                clusterClient.write(msg);
                log.debug("给大厅节点推送消息 node = {}", clusterClient.nodeConfig.getName());
            } catch (Exception e) {
                log.error("推送到所有大厅节点信息异常 node = {}", clusterClient.nodeConfig.getName(), e);
            }
        }

        //通知游戏节点
        for (ClusterClient clusterClient : gameNodes) {
            try {
                clusterClient.write(msg);
                log.debug("给游戏节点推送消息 node = {}", clusterClient.nodeConfig.getName());
            } catch (Exception e) {
                log.error("推送到所有游戏节点信息异常 node = {}", clusterClient.nodeConfig.getName(), e);
            }
        }
    }

    /**
     * 通知当前节点，所有的客户端要展示的跑马灯
     *
     * @param marquee
     */
    private void notifyClientMarquee(Marquee marquee) {
        if (marquee.getType() == GameConstant.Marquee.PLAYER_WIN) {
            if (marquee.getStartTime() < 1) {
                int now = TimeHelper.nowInt();
                marquee.setStartTime(now);
                marquee.setEndTime(now + marquee.getInterval());
            }
        }

        NotifyMarquee notify = new NotifyMarquee();
        notify.marqueeInfo = transMarqueeInfo(marquee);
        // 广播消息
        clusterSystem.broadcastToOnlinePlayer(notify);
        log.debug("通知客户端跑马灯 marquee = {}", JSON.toJSONString(notify));
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
        if(this.marqueeMap == null || this.marqueeMap.isEmpty()) {
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

        log.debug("添加玩家中奖的跑马灯 nick = {},langId = {},gameLangId = {},value = {}", playerNickName, langId, gameLangId,value);

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
}
