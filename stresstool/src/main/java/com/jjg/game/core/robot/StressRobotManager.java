package com.jjg.game.core.robot;

import com.jjg.game.UI.window.frame.MainWindow;
import com.jjg.game.conf.IMainWindow;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.net.connect.GameRobotClient;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.LoggerUtils;
import com.jjg.game.utils.StrEx;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import com.jjg.game.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 压测机器人管理器
 *
 * @author 2CL
 */
public class StressRobotManager {

  /** 机器人ID生成器 */
  private static final AtomicLong ROBOT_ID_GENERATOR = new AtomicLong(0);

  /** 所有的activeRobots */
  private final LinkedHashMap<String, RobotThread> robots = new LinkedHashMap<>();

  private Set<FunctionType> includeActionContainer = new HashSet<>();

  /** 服务器连接失败IP */
  public static String failedServerIp;

  /** 服务器连接失败Port */
  public static int failedServerPort;

  /** 服务器连接失败次数 */
  public static int failedConnectCount;

  /** 启服时间 */
  private long startTime;

  private IMainWindow window;

  /** 关闭按钮同步锁 */
  private final Object stopLock = new Object();

  /** UI更新线程 */
  private static final ExecutorService UI_UPDATE_THREAD = Executors.newFixedThreadPool(2);

  /** 消息发送线程,短时间定时发送 */
  private static ScheduledExecutorService messageSendExecutor;

  public StressRobotManager() {}

  public synchronized boolean containsAction(FunctionType actionKey) {
    return includeActionContainer.contains(actionKey);
  }

  public synchronized boolean exclude(FunctionType actionKey) {
    return includeActionContainer.remove(actionKey);
  }

  public synchronized boolean include(FunctionType actionKey) {
    return includeActionContainer.add(actionKey);
  }

  public synchronized void refill(Set<FunctionType> hashSet) {
    includeActionContainer = new HashSet<>(hashSet);
  }

  public synchronized List<RobotThread> getRobots() {
    List<RobotThread> list = new ArrayList<>();
    Collection<RobotThread> robotThreads = robots.values();
    List<RobotThread> robotlist = new ArrayList<>(robotThreads);
    Collections.shuffle(robotlist);
    int max = 20;
    if (max > robotThreads.size()) {
      max = robotThreads.size();
    }
    int i = 0;
    for (RobotThread robotThread : robotlist) {
      list.add(robotThread);
      i++;
      if (i >= max) {
        break;
      }
    }
    return list;
  }

  public synchronized void addRobot(RobotThread robot) {
    robots.put(robot.getChannel().id().asLongText(), robot);
  }

  public synchronized void remove(String channelId) {
    this.robots.remove(channelId);
  }

  public synchronized void removeRobot(int count) {
    if (count <= 0) {
      return;
    }
    Iterator<Map.Entry<String, RobotThread>> iterator = robots.entrySet().iterator();
    while (iterator.hasNext() && count > 0) {
      Map.Entry<String, RobotThread> kvs = iterator.next();
      kvs.getValue().cancel();
      count--;
      iterator.remove();
    }
  }

  public String uniqueRobotName(String prefix) {
    return prefix + "-" + ROBOT_ID_GENERATOR.incrementAndGet();
  }

  public synchronized int connectedRobotCount() {
    return robots.size();
  }

  public static StressRobotManager instance() {
    return Singleton.INSTANCE.getInstance();
  }

  private enum Singleton {
    INSTANCE;
    final StressRobotManager manager;

    Singleton() {
      this.manager = new StressRobotManager();
    }

    StressRobotManager getInstance() {
      return manager;
    }
  }

  public static void runBackPool(Runnable r) {
    UI_UPDATE_THREAD.execute(r);
  }

  /** 创建并启动机器人 */
  private void robotRun(IMainWindow window) {
    try {
      window.initRobot();

      int proNum = Runtime.getRuntime().availableProcessors();
      proNum = (proNum / 2);
      if (proNum < 1) {
        proNum = 1;
      }

      messageSendExecutor =
          Executors.newScheduledThreadPool(
              proNum,
              r -> {
                Thread thread = new Thread(r);
                thread.setName("scheduledSendPool");
                return thread;
              });

      // 根据当前连接数(机器人数量)动态创建机器人
      while (true) {
        synchronized (this.stopLock) {
          if (!window.isRunning()) {
            break;
          }

          // 创建一个机器人
          int maxConnectedRobot = window.getRunConf().robotConf.getRobotHoldNum();

          if (window.getRunConf().robotConf.getRobotMaxNum() != -1) {
            if (maxConnectedRobot > window.getRunConf().robotConf.getRobotMaxNum()) {
              maxConnectedRobot = window.getRunConf().robotConf.getRobotMaxNum();
            }
          }

          if (StressRobotManager.instance().connectedRobotCount() < maxConnectedRobot) {
            Log4jManager.getInstance()
                .info(
                    window,
                    "登陆,创建机器人.当前数量:"
                        + StressRobotManager.instance().connectedRobotCount()
                        + ",最大数量:"
                        + maxConnectedRobot);
            RobotThread crt =
                RobotThreadFactory.createNewRobot(window, window.getRunConf().robotConf.isSingle());
            crt.initChannel();
          } else if (StressRobotManager.instance().connectedRobotCount() > maxConnectedRobot) {
            Log4jManager.getInstance()
                .info(
                    window,
                    "移除机器人.当前数量:"
                        + StressRobotManager.instance().connectedRobotCount()
                        + ",最大数量:"
                        + maxConnectedRobot);
            int deltaCount =
                StressRobotManager.instance().connectedRobotCount()
                    - window.getRunConf().robotConf.getRobotHoldNum();
            if (deltaCount > 0) {
              // 选择最近最久没有访问的robot移除
              StressRobotManager.instance().removeRobot(deltaCount);
            }
          }
        }
      }
      Log4jManager.getInstance().info(window, "机器人创建线程已停止");
    } catch (Exception e) {
      Log4jManager.getInstance().error(window, e);
    }
  }

  public synchronized void addSendMsgPool(RobotThread robot, SMessage msg, Boolean isContinue) {
    addSendMsgPool(robot, msg, isContinue, true);
  }

  /**
   * 发送消息
   *
   * @param msg 消息
   * @param isContinue 是否继续
   * @param handleReq 是否需要处理请求任务列表.正常功能测试流程是需要处理的,如果只是简单应答服务器则不需要,比如S2CDungeonMsg.ResCheckTime协议
   */
  public synchronized void addSendMsgPool(
      RobotThread robot, SMessage msg, Boolean isContinue, boolean handleReq) {
    if (isContinue) {
      // 依赖调用是否已经设置了跳过状态
      if (!robot.isNowSkip) {
        robot.isNowSkip = true;
        if (robot.requestMultipleEvents.size() != 1) {
          robot.isSkipContiueCount.incrementAndGet();
        }
      }
    } else {
      if (!robot.isNowSkip) {
        robot.isSkipContiueCount.set(0);
      }
    }

    // 需要处理请求任务列表时检测
    if (handleReq) {
      try {
        AbstractEvent<?> abstractEvent = robot.currentEvent;
        if (abstractEvent != null) {
          String functionInfo = abstractEvent.getFunctionInfo();
          if (robot.isSkipContiueCount.get() > 3) {
            Log4jManager.getInstance()
                .warn(
                    robot.getWindow(),
                    "当前功能:"
                        + robot.currentEvent.getFunctionInfo()
                        + " 已经连续:"
                        + robot.isSkipContiueCount.get()
                        + "次手动跳过测试逻辑,可能存在测试漏洞."
                        + ",robotId:"
                        + robot.getPlayer().getPlayerInfo().getPid());
          }
        } else {
          Log4jManager.getInstance().error("abstractEvent is null");
          Log4jManager.getInstance().error(ExceptionEx.currentThreadTraces());
          Log4jManager.getInstance()
              .error(
                  robot.getWindow(),
                  "当前功能:"
                      + "robot.sendingMsg:"
                      + robot.sendingmsg
                      + ",now:"
                      + msg.getId()
                      + ",tid:"
                      + Thread.currentThread().threadId()
                      + ",robot:"
                      + robot.getName()
                      + ",pid:"
                      + robot.getPlayer().getPlayerInfo().getPid()
                      + "resOrder:"
                      + robot.getResOrder()
                      + ",eventState:"
                      + robot.currentEventState);
        }
      } catch (Exception e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(e);
        if (robot.currentEvent == null) {
          stringBuilder.append(" ");
          stringBuilder.append("robot currentEvent is null");
          stringBuilder.append(" ");
          stringBuilder.append(robot.getPlayer().getPlayerInfo().toString());
        }
        Log4jManager.getInstance().error(stringBuilder.toString());
      }
    }

    if (messageSendExecutor != null && !messageSendExecutor.isShutdown()) {
      robot.getWindow().getCtx().addSendLogicMsgs();

      Runnable r =
          () -> {
            try {
              // 不跳过执行即等待返回时才设置
              if (!isContinue) {
                robot.setResOrder(msg.getResOrder());
              }
              long now = System.currentTimeMillis();
              long diffTime = now - robot.sendingTime;
              if (robot.isNowSkip) {
                // 现在是,则不检查
                robot.sendingTime = 0;
              }
              // 压力测试没有严格执行发送间隔.
              if (robot.sendingTime != 0
                  && diffTime < (robot.getWindow().getRunConf().robotConf.getSendDelayTime() / 2)) {

                robot.shortTimeCount++;

                if (MiscUtils.isIdeEnvironment()) {
                  Log4jManager.getInstance()
                      .error(
                          robot.getWindow(),
                          "当前功能:"
                              + robot.currentEvent.getFunctionInfo()
                              + " 压力测试没有严格执行发送间隔. robot.sendingMsg:"
                              + robot.sendingmsg
                              + ",now:"
                              + msg.getId()
                              + ",tid:"
                              + Thread.currentThread().threadId()
                              + ",robot"
                              + robot.getName()
                              + ",time:"
                              + diffTime
                              + "ms,resOrder:"
                              + robot.getResOrder());
                }
              }
              if (robot.isNowSkip) {
                // 下次不是, 同样不检查
                robot.sendingTime = 0;
              } else {
                robot.sendingTime = now;
              }
              robot.isNowSkip = false;
              robot.sendingmsg = msg.getId();
              ChannelFuture future = robot.getChannel().writeAndFlush(msg);
              future.addListener(
                  (ChannelFutureListener)
                      future1 -> {
                        robot.getWindow().getCtx().addCompleteSendMsgs();
                        // 需要处理请求任务列表时检测
                        if (handleReq && isContinue) {
                          robot.sendingTime = 0;
                          robot.run(true);
                        }
                      });
            } catch (Exception e) {
              Log4jManager.getInstance().error(ExceptionEx.e2s(e));
            }
          };

      // long now = System.currentTimeMillis();
      // long _c = now - robot.addTime;
      // if (robot.addTime != 0 && _c < (RunConf.robotConf.getSendDelayTime() / 2)) {
      // Log4jManager.getInstance().error(robot.getWindow(),
      // "压力测试没有严格执行发送间隔. robotaddTimeaddTime.sendingmsg:" + robot.sendingmsg
      // + ",now:" + msg.getId() + ",tid:" + Thread.currentThread().getId()
      // + ",robot" + robot.getName() + ",time:" + _c + ",resOrder:"
      // + robot.getResOrder() + ",enterTimes:" + robot.enterTimes
      // + ",sendList:" + robot.sendList.toString());
      // }
      // if (robot.isNowSkip) {
      // robot.addTime = 0;
      // } else {
      // robot.addTime = now;
      // }
      int size = robot.sendList.size();
      if (size > 1000) {
        robot.sendList.remove(0);
      }
      robot.sendList.add(msg.getId());
      messageSendExecutor.schedule(
          r, robot.getWindow().getRunConf().robotConf.getSendDelayTime(), TimeUnit.MILLISECONDS);
    }
  }

  public boolean isNull() {
    return (messageSendExecutor == null);
  }

  /**
   * @function 运行线程
   */
  private class RunT implements Runnable {

    private IMainWindow window;

    public RunT(IMainWindow window) {
      this.window = window;
    }

    @Override
    public void run() {
      robotRun(this.window);
    }
  }

  public void start(IMainWindow window) {
    this.window = window;
    this.startTime = System.currentTimeMillis();
    UI_UPDATE_THREAD.execute(
        () -> {
          if (!window.isRunning()) {
            synchronized (stopLock) {
              String group = window.getRunConf().choosedServer.getName();
              if ("develop".equals(group)) { // develop服特殊处理，强行设置为只能依次登录
                window.getConsole().setLoginQue(true);
              }
              window.getConsole().addConsoleAreaInfo("开始运行...", false);
              GameRobotClient.init();
              window.setRunning(true);
              window.onStart();
              Log4jManager.logExecute(window);
              if (window instanceof MainWindow) {
                ((MainWindow) window).getRunButton().setEnabled(true);
              }
            }
            new RunT(window).run();
          } else {
            synchronized (stopLock) {
              window.getConsole().addConsoleAreaInfo("开始停止运行...", false);
              if (messageSendExecutor != null) {
                messageSendExecutor.shutdown();
                if (!messageSendExecutor.isTerminated()) {
                  Log4jManager.getInstance().info(window, "等待停止运行机器人发送线程...");
                  try {
                    Thread.sleep(1000);
                  } catch (InterruptedException ex) {
                    Log4jManager.getInstance().error(window, ex);
                  }
                }
                messageSendExecutor = null;
              } else {
                Log4jManager.getInstance().error(window, "sendMsgPool is null !");
              }

              Log4jManager.getInstance().info(window, "socket开始停止...");
              try {
                GameRobotClient.group.shutdownGracefully().sync();
              } catch (InterruptedException ex) {
                Log4jManager.getInstance().error(window, ex);
              }
              GameRobotClient.bootstrap = null;
              Log4jManager.getInstance().info(window, "socket已停止...");
              window.setRunning(false);
              window.onStop();
              Log4jManager.stopLogExecute();
              Log4jManager.getInstance().info(window, "成功停止");
              if (window instanceof MainWindow) {
                ((MainWindow) window).getRunButton().setEnabled(true);
              }
            }
          }
        });
  }

  public IMainWindow getWindow() {
    return window;
  }

  public long getStartTime() {
    return startTime;
  }

  /**
   * 版本信息
   *
   * <p>第一行为jar版本 第二行为打包时间 第三行为构建版本的人
   *
   * @param i 第几行
   * @return e
   */
  public String getGameVersionInfo(int i) {
    InputStream is = null;
    BufferedReader br = null;

    try {
      is = getClass().getClassLoader().getResourceAsStream("build_info.properties");
      br = new BufferedReader(new InputStreamReader(is));
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return "unknown";
    }

    String version = null;
    int k = 1;
    try {
      while ((version = br.readLine()) != null) {
        // 读第一行
        if (i == k++) {
          break;
        }
      }
    } catch (IOException e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
    if (version == null || version.isEmpty()) {
      return "";
    }
    return StrEx.removeLeft(version, version.indexOf("_") + 1);
  }
}
