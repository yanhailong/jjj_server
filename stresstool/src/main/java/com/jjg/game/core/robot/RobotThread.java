package com.jjg.game.core.robot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.Message.Builder;

import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.conf.IMainWindow;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.messages.MessageEntityWrapper;
import com.jjg.game.core.net.connect.GameRobotClient;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.logic.login.req.ReqLoginEvent;
import io.netty.channel.Channel;
import com.jjg.game.logic.robot.entity.RobotPlayer;
import com.jjg.game.utils.RandomUtil;
import com.jjg.game.utils.ProtoBufUtils;
import com.jjg.game.utils.ProtoBufUtils.ProtoClientMessage;
import org.apache.commons.lang.reflect.ConstructorUtils;

/**
 * @author 2CL
 * @function 客户端机器人线程(方法)
 */
public class RobotThread {

    private final Map<FunctionType, FunctionExecuteStatistics> functionExecuteStatistics = new HashMap<>();
    private final IMainWindow window;
    /**
     * 单接口执行事件列队
     */
    private final Queue<Map<Integer, Builder>> reqBuilder = new LinkedBlockingQueue<>();
    /**
     * 机器人可重复请求事件
     */
    public Map<FunctionType, Map<Integer, AbstractEvent<?>>> requestMultipleEvents = new HashMap<>();
    public int shortTimeCount;
    public boolean isInCleanBag = false;
    public int roundNumber = 1;
    public boolean init = true;
    public int eventCount = 0;
    public volatile boolean block = true;
    /**
     * 机器人通信用管道
     */
    public Channel channel;
    /**
     * 测试用字段
     */
    public long sendingTime = 0;
    public int sendingmsg;
    /**
     * 消息添加到队列的时间
     */
    public long addTime;
    /**
     * 进入游戏的次数
     */
    public int enterTimes;
    public String token;
    public ArrayList<Integer> sendList = new ArrayList<Integer>();
    public ArrayList<Integer> recvList = new ArrayList<Integer>();
    /**
     * 当前指令是否跳出等待执行
     */
    public volatile boolean isNowSkip;
    /**
     * 连续跳出次数
     */
    public volatile AtomicLong isSkipContiueCount = new AtomicLong();
    public volatile AbstractEvent currentEvent;
    /**
     * 观察字段变化,追踪Event状态异常用
     */
    public volatile int currentEventState = 0;
    /**
     * 当前连接的服务器ip
     */
    public String serverIp;
    /**
     * 当前连接的服务器port
     */
    public int serverPort;
    /**
     * 游戏服id
     */
    public int serverId;
    /**
     * 是否所有功能模块已执行完毕
     */
    private boolean isAllFunOver;
    /**
     * 停止任务标记
     */
    private volatile boolean cancel = false;
    /**
     * 注册的当前等待回调
     */
    private int resOrder;
    /**
     * 机器人自身消息序列号(协议用)
     */
    private int magicNum = 0;
    /**
     * 请求事件队列
     */
    private Queue<AbstractEvent<?>> reqQueue = new LinkedBlockingQueue<>();
    /**
     * 预先(登陆流程)需要执行事件的队列
     */
    private Queue<AbstractEvent<?>> preReqQueue = new LinkedBlockingQueue<>();
    /**
     * 机器人响应事件Map
     */
    private Map<Integer, AbstractEvent<?>> responseEvents = new HashMap<>();
    /**
     * 机器人账号
     */
    private String accountId;
    /**
     * 机器人编号
     */
    private long robotNumber;
    private long createTime;
    /**
     * 机器人数据承载对象
     */
    private RobotPlayer player;
    private String lastFunName;

    /**
     * 构造器人线程
     *
     * @param window
     */
    RobotThread(String accountId, long robotNumber, IMainWindow window) {
        this.window = window;
        this.accountId = accountId;
        this.robotNumber = robotNumber;
        this.player = new RobotPlayer(this);
        this.roundNumber = RandomUtil.nextInt(window.getMinRoundNumber(), window.getMaxRoundNumber());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean cancelled() {
        return cancel;
    }

    public void cancel() {
        if (!cancel) {
            cancel = true;
        }
    }

    public IMainWindow getWindow() {
        return window;
    }

    /**
     * 提取消息序列
     */
    public int getAndAddMagicNum() {
        return ++magicNum;
    }

    /**
     * 提取消息序列
     */
    public int getMagicNum() {
        return magicNum;
    }

    /**
     * 初始化机器人事件
     */
    void initEvents(Map<Integer, AbstractEvent<?>> requireOnceEvents, Map<Integer, AbstractEvent<?>> responseEvents, Map<FunctionType, Map<Integer, AbstractEvent<?>>> requestMultipleEvents, Map<FunctionType, Map<Integer, AbstractEvent<?>>> requestOnceFunctionEvents) {

        this.preReqQueue = new LinkedBlockingQueue<>();
        this.reqQueue = new LinkedBlockingQueue<>();
        this.requestMultipleEvents = new HashMap<>();
        this.responseEvents = new HashMap<>();

        Iterator<Entry<Integer, AbstractEvent<?>>> iterator = requireOnceEvents.entrySet().iterator();
        iterator.forEachRemaining(event -> {
            preReqQueue.offer(event.getValue());
            // System.out.println("-preReqQueue---event=" + event.getValue());
        });

        Iterator<Entry<FunctionType, Map<Integer, AbstractEvent<?>>>> iteratorOut = requestOnceFunctionEvents.entrySet().iterator();
        iteratorOut.forEachRemaining(actionOut -> {
            Map<Integer, AbstractEvent<?>> eventMap = actionOut.getValue();
            Iterator<Entry<Integer, AbstractEvent<?>>> iteratorIn = eventMap.entrySet().iterator();
            iteratorIn.forEachRemaining(actionIn -> reqQueue.offer(actionIn.getValue()));
        });

        this.requestMultipleEvents = requestMultipleEvents;
        this.responseEvents = responseEvents;
    }

    /**
     * 创建机器人连接
     */
    public void initChannel() {
        try {
            GameRobotClient.run(window, this);
        } catch (Exception e) {
            Log4jManager.getInstance().error(window, e);
        }
    }

    private void onAllOver() {
        long now = System.currentTimeMillis();
        long diff = now - this.createTime;
        int usedSecond = (int) (diff / 1000);
        int minute = usedSecond / 60;
        int second = usedSecond % 60;
        // 如果有分钟,就把分钟打印出来
        if (minute > 0) {
            Log4jManager.getInstance().info(window, this.accountId + " 所有功能模块执行完毕,共执行请求事件" + eventCount + "次,执行轮数" + roundNumber + ",耗时:" + minute + "分钟" + second + "秒");
        } else {
            Log4jManager.getInstance().info(window, this.accountId + " 所有功能模块执行完毕,共执行请求事件" + eventCount + "次,执行轮数" + roundNumber + ",耗时:" + second + "秒");
        }
    }

    private void innerRun(boolean isSkip) {
        this.currentEventState = 1;
        if (isSkip) {
            this.isNowSkip = true;
        }
        if (isSkip) {
            if (requestMultipleEvents.size() != 1 && !(window.getStressTestType() == 1)) {
                this.isSkipContiueCount.incrementAndGet();
            }
        } else {
            this.isSkipContiueCount.set(0);
        }
        try {
            if (window.getStressTestType() == 2) {
                long maxWaitTime = System.currentTimeMillis() + 10000L;
                while (window.isPause() || System.currentTimeMillis() > maxWaitTime) {
                }
            }
            if (channel == null) {
                return;
            }
            if (!channel.isOpen()) {
                channel.close();
                return;
            }
            if (!window.isRunning()) {
                return;
            }
            // 安全关闭robot任务
            if (this.cancelled()) {
                Log4jManager.getInstance().info(window, this.getName() + " tasks are cancelled.");
                channel.close();
                return;
            }
            // 预执行事件
            if (!preReqQueue.isEmpty() && !this.cancelled()) {
                runPreReqQueueEvent();
                return;
            }
            // 安全关闭robot任务
            if (this.cancelled()) {
                Log4jManager.getInstance().info(window, this.getName() + " tasks are cancelled.");
                channel.close();
                return;
            }
            if (window.getStressTestType() == 2) {
                // 运行功能请求事件
                runFunctionReqMsgEvent();
            } else if (window.getStressTestType() == 1) {
                // 运行自选请求消息事件
                runSelectReqMsgEvent();
            } else {
                Log4jManager.getInstance().error(window, "未知的功能类型!!!!!!");
            }
        } catch (Exception e) {
            Log4jManager.getInstance().error(window, e);
        }
        this.currentEventState = 2;
    }

    /**
     * 运行预请求消息队列
     */
    private void runPreReqQueueEvent() {
        AbstractEvent<?> event = preReqQueue.peek();
        if (event == null) {
            return;
        }
        // 没有登录成功就继续等待
        // TODO login
        if (!this.getPlayer().getIsLogin() && event.getClass() != ReqLoginEvent.class) {
            return;
        }
        preReqQueue.poll();
        this.currentEvent = event;
        eventCount++;
        event.doAction(MessageEntityWrapper.msgEntityWrapper(event));
    }

    /**
     * 运行功能请求消息事件
     */
    private void runFunctionReqMsgEvent() {
        if (reqQueue.isEmpty()) {
            if (!requestMultipleEvents.isEmpty()) {
                // all the function types
                ArrayList<FunctionType> functionEnumTypes = new ArrayList<FunctionType>();
                // repeatable events
                Object[] repeatedEventsKeyCopy = requestMultipleEvents.keySet().toArray();
                // copy all the repeatable functions proto into list
                for (Object o : repeatedEventsKeyCopy) {
                    functionEnumTypes.add((FunctionType) o);
                }
                // 打乱功能模块顺序
                if (functionEnumTypes.size() > 1) {
                    if (RandomUtil.getRandomBoolean100(70)) {
                        Collections.shuffle(functionEnumTypes);
                    }
                }
                Map<Integer, AbstractEvent<?>> eventMap = null;
                Iterator<Entry<Integer, AbstractEvent<?>>> iteratorIn = null;
                boolean isAllOver = true;
                for (FunctionType function : functionEnumTypes) {
                    // 检查是否已经执行了指定次数
                    if (exceedLimitCount(function)) {
                        continue;
                    }
                    // 将选中的功能放入请求队列中
                    eventMap = requestMultipleEvents.get(function);
                    iteratorIn = eventMap.entrySet().iterator();
                    while (iteratorIn.hasNext()) {
                        reqQueue.offer(iteratorIn.next().getValue());
                    }
                    // 添加功能模块已执行次数
                    addDoCount(function);
                    isAllOver = false;
                }
                if (isAllOver) {
                    isAllFunOver = true;
                }
            } else {
                isAllFunOver = true;
            }
            if (isAllFunOver) {
                // 换下一个机器人来执行
                onAllOver();
                channel.close();
                return;
            }
        }
        if (!reqQueue.isEmpty()) {
            AbstractEvent<?> event = reqQueue.poll();
            this.currentEvent = event;
            if (this.lastFunName != null && this.currentEvent == null) {
                Log4jManager.getInstance().error(window, "currentEvent is null");
            } else if (this.lastFunName != null && this.currentEvent.getFunctionInfo() == null) {
                Log4jManager.getInstance().error(window, "currentEvent.getFunctionInfo() is null:" + currentEvent.getClass());
            }
            if (this.lastFunName != null && !this.currentEvent.getFunctionInfo().equals(this.lastFunName)) {
                this.isSkipContiueCount.set(0);
            }
            this.lastFunName = this.currentEvent.getFunctionInfo();
            eventCount++;
            event.doAction(MessageEntityWrapper.msgEntityWrapper(event));
        }
    }

    private void runSelectReqMsgEvent() throws Exception {
        // 自己选的ClientMessage集合
        List<ProtoClientMessage> protoClientMessages = null;
        // 当请求的builder 为空值时 重新加入builder
        if (reqBuilder.isEmpty()) {
            // TODO 现在不支持 随机协议
            if (window.getMessageId() == null) {
                // 随机取出一个协议 并 填充 发送
                ProtoClientMessage message = EventScanner.getClientMessages().get(RandomUtil.nextInt(EventScanner.getClientMessages().size()));
                protoClientMessages = new ArrayList<>();
                protoClientMessages.add(message);
            } else {
                // 获取选择的协议号 根据输入的值重新封装builder
                String[] messages = window.getMessageId().split(",");
                for (String message : messages) {
                    Map<Integer, ProtoClientMessage> map = EventScanner.getClientMessageMap();
                    ProtoClientMessage pmessage = map.get(Integer.parseInt(message));
                    if (protoClientMessages == null) {
                        protoClientMessages = new ArrayList<>();
                    }
                    protoClientMessages.add(pmessage);
                }
            }
            // 创建builder 并设置值
            int index = 0;
            String value = window.getMessageInfo();
            for (ProtoClientMessage protoClientMessage : protoClientMessages) {
                Builder builder = ProtoBufUtils.createBuilder(protoClientMessage.getBuilder().getClass());
                ProtoBufUtils.setBuilderVaules(builder, value, index);
                Map<Integer, Builder> map = new HashMap<>();
                map.put(protoClientMessage.getMsgId(), builder);
                reqBuilder.add(map);
                index++;
            }
        }
        Map<Integer, Builder> show = reqBuilder.poll();
        if (show != null) {
            show.forEach((messageId, builder) -> {
                // 发送请求
                SMessage msg = new SMessage(messageId, builder.build().toByteArray());
                StressRobotManager.instance().addSendMsgPool(this, msg, true);
            });
        }
    }

    /**
     * 执行机器人线程
     *
     * @param isSkip 跳过等待执行间隔,立即执行下一条指令
     */
    public void run(boolean isSkip) {
        innerRun(isSkip);
    }

    /**
     * 添加响应消息
     */
    public void addRespMsg(SMessage receiveMsg) {
        this.response(receiveMsg);
    }

    /**
     * 机器人响应消息事件
     */
    private void response(SMessage msg) {

        AbstractEvent<?> responseEvent = responseEvents.get(msg.getId());
        if (msg.getId() < 0) {
            return;
        }
        if (null == responseEvent) {
            return;
        }

        int size = responseEvent.robot.recvList.size();
        if (size > 1000) {
            responseEvent.robot.recvList.removeFirst();
        }
        responseEvent.robot.recvList.add(msg.getId());
        // responseEvent.robot.currentEvent = responseEvent;
        if (!msg.getStatus().isEmpty()) {
            Log4jManager.getInstance().debug(window, responseEvent.getClass().getSimpleName() + "-->" + "消息id:" + msg.getId() + " errorcode:" + msg.getStatus() + "\n");
        }
        Class<?> respEntityClass = MessageEntityWrapper.getMsgEntityClass(responseEvent);
        Object respEntity = ProtostuffUtil.deserialize(msg.getData(), respEntityClass);
        // 应该用错误码判断消息是否异常 而不是对data判空 如果data为空则应检查具体开发的功能逻辑
        responseEvent.doAction(respEntity, msg.getStatus());
        // 通过回调执行数据驱动
        int resOrder = responseEvent.robot.getResOrder();
        if ((resOrder != -1) && (resOrder != -2) && (resOrder == msg.getId())) {
            responseEvent.robot.run(false);
        }
    }

    /**
     * 检查某个功能模块是否已经达到限定次数
     */
    private boolean exceedLimitCount(FunctionType type) {

        if (window.getStressTestType() == 2 && window.isUnLimitTimes()) {
            return false;
        }

        // 如果包含该模块就继续执行，直接设置成执行次数达到上限,已达到类似移除了该功能的效果，
        // 下次再增加进来的时候会继续执行没有执行完的次数
        if (!StressRobotManager.instance().containsAction(type)) {
            Log4jManager.getInstance().info(window, this.getName() + ":" + "停止执行消息: " + type.fName);
            if (functionExecuteStatistics.containsKey(type)) {
                functionExecuteStatistics.get(type).setPaused(true);
            }
            return true;
        }

        if (StressRobotManager.instance().containsAction(type)) {
            if (functionExecuteStatistics.containsKey(type)) {
                functionExecuteStatistics.get(type).isPaused();
                // Log4jManager.getInstance().info(this.getName() + ":" + "继续执行消息: " + type.fName);
                functionExecuteStatistics.get(type).setPaused(false);
            }
        }

        if (type.fNum <= 0) {
            return false;
        }
        if (!functionExecuteStatistics.containsKey(type)) {
            return false;
        }
        Integer count = functionExecuteStatistics.get(type).getExecuteAmount();

        return count >= (type.fNum * roundNumber);
    }

    /**
     * 添加某个功能模块已执行次数
     */
    private void addDoCount(FunctionType type) {
        functionExecuteStatistics.putIfAbsent(type, new FunctionExecuteStatistics(type));
        functionExecuteStatistics.get(type).addExecuteAmount();
        functionExecuteStatistics.get(type).setLastExecuteTimestamp(System.currentTimeMillis());
    }

    /**
     * 删除机器人的当前功能,并清空当前的发送队列.
     */
    public void removeCurrentFun() {
        FunctionType currentType = currentEvent.getFunctionType();
        this.requestMultipleEvents.remove(currentType);
        this.reqQueue.clear();
    }

    /**
     * 数据驱动指定回调
     */
    public int getResOrder() {
        return this.resOrder;
    }

    /**
     * 数据驱动指定回调
     */
    public void setResOrder(int resOrder) {
        this.resOrder = resOrder;
    }

    public Queue<AbstractEvent<?>> getReqQueue() {
        return reqQueue;
    }

    public Map<Integer, AbstractEvent<?>> getResponseEvents() {
        return responseEvents;
    }

    public void setResponseEvents(Map<Integer, AbstractEvent<?>> responseEvents) {
        this.responseEvents = responseEvents;
    }

    public String getName() {
        return accountId;
    }

    public void setName(String name) {
        this.accountId = name;
    }

    public Channel getChannel() {
        return channel;
    }

    public RobotPlayer getPlayer() {
        return player;
    }

    public void setPlayer(RobotPlayer player) {
        this.player = player;
    }

    /**
     * 是否所有功能执行完毕
     */
    public boolean isAllFunOver() {
        return isAllFunOver;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getAccountId() {
        return accountId;
    }

    /**
     * 机器人各个功能块已经执行的次数
     */
    private class FunctionExecuteStatistics {

        @SuppressWarnings("unused")
        private FunctionType functionType;

        private Integer executeAmount;

        @SuppressWarnings("unused")
        private Long lastExecuteTimestamp;
        private boolean paused;

        public FunctionExecuteStatistics(FunctionType functionType) {
            this.functionType = functionType;
            this.executeAmount = 0;
            this.lastExecuteTimestamp = 0L;
        }

        public boolean isPaused() {
            return paused;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        public Integer getExecuteAmount() {
            return executeAmount;
        }

        public void addExecuteAmount() {
            this.executeAmount += 1;
        }

        public void setLastExecuteTimestamp(Long lastExecuteTimestamp) {
            this.lastExecuteTimestamp = lastExecuteTimestamp;
        }
    }
}
