package com.jjg.game.core.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.MarqueeDao;
import com.jjg.game.core.data.Marquee;
import com.jjg.game.core.pb.NotifyMarquee;
import com.jjg.game.core.pb.NotifyStopMarquee;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/8/13 13:49
 */
public abstract class AbstractMarqueeManager implements TimerListener {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected MarqueeDao marqueeDao;
    @Autowired
    protected TimerCenter timerCenter;
    @Autowired
    protected MarsCurator marsCurator;
    @Autowired
    protected NodeConfig nodeConfig;
    @Autowired
    protected ClusterSystem clusterSystem;

    //排序后的跑马灯列表,  在列表越靠后，越优先
    private LinkedList<Marquee> sortedMarquees;
    //映射表
    private Map<Integer,Marquee> marqueeMap;
    //当前正在运行的跑马灯
    protected int nowRunMarqueeId;

    protected TimerEvent<String> checkEvent;

    public void init(){
        loadAllmarquee();
        addCheckEvent();
    }

    private void addCheckEvent(){
        this.checkEvent = new TimerEvent<>(this, "checkEvent",3).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(this.checkEvent);
    }

    /**
     * 加载所有的跑马灯信息
     */
    protected void loadAllmarquee(){
        List allMarquee = marqueeDao.getAllMarquee();
        if(allMarquee == null || allMarquee.isEmpty()){
            return;
        }

        Map<Integer, Marquee> tmpMarqueeMap = new HashMap<>();
        List<Marquee> tmpList = new ArrayList<>();
        for(Object o : allMarquee){
            Marquee marquee = (Marquee) o;
            tmpList.add(marquee);
            tmpMarqueeMap.put(marquee.getId(),marquee);
        }

        this.sortedMarquees = sortMarquee(tmpList);
        this.marqueeMap = tmpMarqueeMap;

        log.debug("初始加载跑马灯后打印 map.size = {}", this.marqueeMap.size());
        for(Marquee m : this.sortedMarquees){
            System.out.println(JSON.toJSONString(m));
        }
    }

    /**
     * 添加信息的跑马灯后要进行排序
     * @param marquee
     */
    public void addNewMarquee(Marquee marquee){
        if(this.marqueeMap == null){
            this.marqueeMap = new HashMap<>();
        }
        if(this.marqueeMap.containsKey(marquee.getId())){
            this.marqueeMap.put(marquee.getId(),marquee);
            return;
        }

        List<Marquee> marqueeList = null;
        if(this.sortedMarquees != null){
            marqueeList = new ArrayList<>(this.sortedMarquees);
        }else {
            marqueeList = new ArrayList<>();
        }
        marqueeList.add(marquee);
        this.sortedMarquees = sortMarquee(marqueeList);
        this.marqueeMap.put(marquee.getId(),marquee);

        log.debug("添加跑马灯后打印 map.size = {}", this.marqueeMap.size());
        for(Marquee m : this.sortedMarquees){
            System.out.println(JSON.toJSONString(m));
        }
    }

    /**
     * 删除跑马灯后要进行排序
     * @param id
     */
    public void removeMarquee(int id){
        if(id < 1){
            return;
        }

        if(this.sortedMarquees == null || this.sortedMarquees.isEmpty()){
            return;
        }

        removeFromeRedis(id);
        List<Marquee> marqueeList = new ArrayList<>(this.sortedMarquees);
        marqueeList.removeIf(marquee -> marquee.getId() == id);
        this.sortedMarquees = sortMarquee(marqueeList);
        this.marqueeMap.remove(id);

        addNotifyNodeStopEvent(id);
        addNotifyStopEvent(id);

        log.debug("删除跑马灯后打印 map.size = {}", this.marqueeMap.size());
        for(Marquee m : this.sortedMarquees){
            System.out.println(JSON.toJSONString(m));
        }
    }

    /**
     * 删除跑马灯
     * @param id
     */
    protected void removeFromeRedis(int id){
        if(marsCurator.master(NodeType.HALL.getValue()) || nodeConfig.getName().equals("HALL_SHIYI")){
            marqueeDao.removeMarquee(id);
            log.debug("从redis删除跑马灯 id = {}",id);
        }
    }

    /**
     *
     * 进行排序
     * @param list
     * @return
     */
    protected LinkedList<Marquee> sortMarquee(List<Marquee> list){
        //进行排序
        return list
                .stream()
                .sorted(Comparator
                        .comparingInt(Marquee::getType).reversed()  // 先按 type 降序
                        .thenComparingInt(Marquee::getPriority)     // 再按 priority 升序
                )
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * 检查跑马灯列表
     */
    public void check(){
        if(this.sortedMarquees == null || this.sortedMarquees.isEmpty()){
            return;
        }
        log.debug("nowRunMarqueeId = {}", nowRunMarqueeId);
        // 获取从尾部开始的 ListIterator
        ListIterator<Marquee> it = this.sortedMarquees.listIterator(this.sortedMarquees.size());
        int now = TimeHelper.nowInt();
        while (it.hasPrevious()){
            Marquee marquee = it.previous();
            if(now > marquee.getEndTime()){
                //删除过期的跑马灯
                it.remove();
                this.marqueeMap.remove(marquee.getId());
                removeFromeRedis(marquee.getId());
                log.debug("移除过期跑马灯 id = {}",marquee.getId());
                return;
            }

            if(now < marquee.getStartTime()){
                return;
            }

            if(this.nowRunMarqueeId != marquee.getId()){
                this.nowRunMarqueeId = marquee.getId();
                addNotifySendEvent(marquee.getId());
            }
            break;
        }
    }

    @Override
    public void onTimer(TimerEvent e) {
        if(e == this.checkEvent){
            check();
        }else {
            String[] arr = e.getParameter().toString().split("_");
            int id = Integer.parseInt(arr[1]);

            if("notifySendEvent".equals(arr[0])){
                Marquee marquee = this.marqueeMap.get(id);
                if(marquee == null){
                    return;
                }
                notifyClientMarquee(marquee);
            }else if("notifyStopEvent".equals(arr[0])){
                notifyClientStopMarquee(id);
            }else if("notifyNodeSendEvent".equals(arr[0])){
                Marquee marquee = this.marqueeMap.get(id);
                if(marquee == null){
                    return;
                }

                NotifyAllNodesMarqueeServer notify = new NotifyAllNodesMarqueeServer();
                notify.id = marquee.getId();
                notify.content = marquee.getContent();
                notify.interval = marquee.getInterval();
                notify.startTime = marquee.getStartTime();
                notify.endTime = marquee.getEndTime();
                log.debug("通知跑马灯信息 id = {}",id);
                notifyHallAndGameNodeStartMarquee(notify);
                notifyClientMarquee(marquee);
            }else if("notifyNodeStopEvent".equals(arr[0])){
                NotifyAllNodesStopMarqueeServer notify = new NotifyAllNodesStopMarqueeServer();
                notify.id = id;
                log.debug("通知停止跑马灯信息 id = {}",id);
                notifyHallAndGameNodeStopMarquee(notify);
                notifyClientStopMarquee(id);
            }
        }
    }

    /**
     * 通知客户端开始跑马灯
     * @param id
     */
    protected void addNotifySendEvent(long id){
        TimerEvent<String> nodeEvent = new TimerEvent<>(this, 1, "notifySendEvent_" + id).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(nodeEvent);
        log.debug("添加通知客户端跑马灯事件 id = {}",id);
    }

    /**
     * 通知客户端停止跑马灯
     * @param id
     */
    protected void addNotifyStopEvent(long id){
        TimerEvent<String> nodeEvent = new TimerEvent<>(this, 1, "notifyStopEvent_" + id).withTimeUnit(TimeUnit.SECONDS);
        this.timerCenter.add(nodeEvent);
        log.debug("添加通知客户端停止跑马灯事件 id = {}",id);
    }

    /**
     * 通知其他节点停止跑马灯
     * @param id
     */
    protected void addNotifyNodeStopEvent(long id){
        if(marsCurator.master(NodeType.HALL.getValue())){
            TimerEvent<String> nodeEvent = new TimerEvent<>(this, 1, "notifyNodeStopEvent_" + id).withTimeUnit(TimeUnit.SECONDS);
            this.timerCenter.add(nodeEvent);
            log.debug("添加通知节点停止跑马灯事件 id = {}",id);
        }
    }

    /**
     * 通知所有的大厅和游戏节点开始跑马灯
     */
    public void notifyHallAndGameNodeStartMarquee(NotifyAllNodesMarqueeServer notify){
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        notifyHallAndGameNode(pfMessage);
    }

    /**
     * 通知所有的大厅和游戏节点开始跑马灯
     */
    public void notifyHallAndGameNodeStopMarquee(NotifyAllNodesStopMarqueeServer notify){
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        notifyHallAndGameNode(pfMessage);
    }

    /**
     * 通知所有的大厅和游戏节点
     */
    protected void notifyHallAndGameNode(PFMessage pfMessage){
        ClusterMessage msg = new ClusterMessage(pfMessage);

        //获取大厅节点
        List<ClusterClient> hallNodes = ClusterSystem.system.getNodesByType(NodeType.HALL);
        //获取游戏节点
        List<ClusterClient> gameNodes = ClusterSystem.system.getNodesByType(NodeType.GAME);

        //通知大厅节点
        for (ClusterClient clusterClient : hallNodes) {
            try {
                clusterClient.write(msg);
                log.debug("给大厅节点推送消息 node = {}",clusterClient.nodeConfig.getName());
            } catch (Exception e) {
                log.error("推送到所有大厅节点信息异常 node = {}",clusterClient.nodeConfig.getName(), e);
            }
        }

        //通知游戏节点
        for (ClusterClient clusterClient : gameNodes) {
            try {
                clusterClient.write(msg);
                log.debug("给游戏节点推送消息 node = {}",clusterClient.nodeConfig.getName());
            } catch (Exception e) {
                log.error("推送到所有游戏节点信息异常 node = {}",clusterClient.nodeConfig.getName(),e);
            }
        }
    }

    /**
     * 通知当前节点，所有的客户端要展示的跑马灯
     * @param marquee
     */
    protected void notifyClientMarquee(Marquee marquee){
        NotifyMarquee notify = new NotifyMarquee();
        notify.id = marquee.getId();
        notify.content = marquee.getContent();
        notify.interval = marquee.getInterval();
        notify.startTime = marquee.getStartTime();
        notify.endTime = marquee.getEndTime();
        clusterSystem.sessionMap().entrySet().forEach(en -> en.getValue().send(notify));
    }

    /**
     * 通知当前节点，所有的客户端要停止的跑马灯
     * @param id
     */
    protected void notifyClientStopMarquee(int id){
        NotifyStopMarquee notify = new NotifyStopMarquee();
        notify.id = id;
        clusterSystem.sessionMap().entrySet().forEach(en -> en.getValue().send(notify));
    }

    /**
     * 获取当前正在运行的跑马灯
     * @return
     */
    public Marquee getCurrentMarquee(){
        if(this.nowRunMarqueeId < 1){
            return null;
        }

        return this.marqueeMap.get(this.nowRunMarqueeId);
    }
}
