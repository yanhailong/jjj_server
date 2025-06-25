package com.jjg.game.core;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.jjg.game.core.entity.GarbageCollectorInfo;
import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.LoggerUtils;
import org.slf4j.Logger;

import com.jjg.game.UI.window.frame.FunctionWindow;
import com.jjg.game.conf.IMainWindow;
import com.jjg.game.core.robot.StressRobotManager;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.core.robot.RobotThreadFactory;

/**
 * @author Administrator
 */
public class Log4jManager {

  public static boolean netDetail = true;
  // TODO 消息是否出错了,有没有错误返回码
  public static boolean netSucc = true;
  public static boolean netClose = true;

  static long lastSendMsgs = -2;
  static long llastSendMsgs = -1;

  public static Log4jManager getInstance() {
    return INSTANCE;
  }

  public void init() {
    try {
      CLIENT = LoggerUtils.LOGGER;
    } catch (Exception e) {
      System.out.println(ExceptionEx.e2s(e));
      System.exit(1);
    }
  }

  public boolean isInit() {
    return (CLIENT != null);
  }

  public boolean isDebugEnabled() {
    if (CLIENT == null) {
      return false;
    }
    return CLIENT.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    if (CLIENT == null) {
      return false;
    }
    return CLIENT.isInfoEnabled();
  }

  public void debug(IMainWindow window, String str) {
    if (CLIENT == null) {
      System.out.println(str);
    } else {
      CLIENT.debug(str);
    }
    window.getConsole().addConsoleAreaInfo("debug:" + str, true);
  }

  public void info(IMainWindow window, String str) {
    if (CLIENT == null) {
      System.out.println(str);
    } else {
      CLIENT.info(str);
    }
    window.getConsole().addConsoleAreaInfo(str, false);
  }

  public void warn(String str) {
    warn(null, str);
  }

  public void warn(IMainWindow window, String str) {
    if (CLIENT == null) {
      System.out.println(str);
    } else {
      CLIENT.warn(str);
    }
    if (window != null) {
      window.getConsole().addConsoleAreaInfo("警告:" + str, false);
    }
  }

  public void error(Exception e) {
    error(null, ExceptionEx.e2s(e));
  }

  public void error(String str) {
    error(null, str);
  }

  public void error(IMainWindow window, Exception e) {
    error(window, ExceptionEx.e2s(e));
  }

  public void error(IMainWindow window, String str) {
    if (CLIENT == null) {
      System.out.println(str);
    } else {
      CLIENT.error(str);
    }
    if (window != null) {
      window.getConsole().addConsoleAreaInfo("错误:" + str, false);
    }
  }

  static class PrintUtil {

    static long sendLogicMsgs = 0;
    static long sendMsgs = 0;
    static long recvMsgs = 0;

    static long chasmSendMsgs = 0;
    static long chasmCompleteSendMsgs = 0;
    static long chasmRecvMsgs = 0;
  }

  static class StatisticsUtil {

    static int accumTimes = 0;
    static long sendTimes = 0;
    static long statisticTime = 0;
  }

  public static void logExecute(IMainWindow window) {
    scheduledSinglePoolLog =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread thread = new Thread(r);
              thread.setName("scheduledSinglePoolMin1");
              return thread;
            });

    Runnable r =
        () -> {
          try {
            long sendLogicMsgs = window.getCtx().getSendLogicMsgs();
            long sendMsgs = window.getCtx().getSendMsgs();
            long recvMsgs = window.getCtx().getReceiveMsgs();
            long sendMsgsT = sendMsgs;

            if (window.getConsole().isIncre()) {
              long _temp = sendLogicMsgs;
              long _temp2 = sendMsgs;
              long _temp3 = recvMsgs;
              sendLogicMsgs = sendLogicMsgs - PrintUtil.sendLogicMsgs;
              sendMsgs = sendMsgs - PrintUtil.sendMsgs;
              recvMsgs = recvMsgs - PrintUtil.recvMsgs;

              PrintUtil.sendLogicMsgs = _temp;
              PrintUtil.sendMsgs = _temp2;
              PrintUtil.recvMsgs = _temp3;
            }

            // 每分钟统计一次
            long now = System.currentTimeMillis();
            if (StatisticsUtil.statisticTime == 0) {
              StatisticsUtil.statisticTime = now;
              StatisticsUtil.sendTimes = sendMsgsT;
            } else if (now - StatisticsUtil.statisticTime > 60000) {
              long lsendtimes = StatisticsUtil.sendTimes;
              StatisticsUtil.statisticTime = now;
              StatisticsUtil.sendTimes = sendMsgsT;

              long fazhi =
                  (long)
                      (60
                          / (window.getRunConf().robotConf.getSendDelayTime() / 1000)
                          * window.getRunConf().robotConf.getRobotHoldNum());
              // 每分钟发送消息数量小于400累加
              long count = sendMsgsT - lsendtimes;
              if (count < fazhi / 2) {
                StatisticsUtil.accumTimes++;
                Log4jManager.getInstance()
                    .warn(
                        window,
                        "消息发送次数 错误统计加1,发送次数【"
                            + count
                            + "】"
                            + "已持续【"
                            + StatisticsUtil.accumTimes
                            + "】分钟"
                            + " 当前的发送数量： "
                            + count
                            + " 配置需求发送的数量："
                            + fazhi);
              } else {
                StatisticsUtil.accumTimes = 0;
              }
              // 累计5次记录日志
              if (StatisticsUtil.accumTimes >= 5) {
                String name = ManagementFactory.getRuntimeMXBean().getName();
                String pid = name.split("@")[0];
                String funcGroup =
                    window.getRunConf().funcGroups == null
                        ? ""
                        : window.getRunConf().funcGroups.toString();

                Log4jManager.getInstance()
                    .error(
                        window,
                        "机器人分组索引: "
                            + System.getProperty("groups")
                            + " 机器人进程id:"
                            + pid
                            + "当前压测功能的每分钟发送消息数量比预期过低. 应发送:["
                            + fazhi
                            + "] , 实际发送["
                            + count
                            + "] "
                            + ",已持续["
                            + StatisticsUtil.accumTimes
                            + "] 分钟"
                            + ". 当前功能分组名:"
                            + funcGroup
                            + ",当前账号前缀:"
                            + window.getRunConf().robotConf.getPrefixName()
                            + " id:"
                            + RobotThreadFactory.counter
                            + "应在线:"
                            + window.getRunConf().robotConf.getRobotHoldNum()
                            + "实际登陆:"
                            + window.getCtx().getLogined());

                List<RobotThread> list = StressRobotManager.instance().getRobots();
                String funs = "";
                for (RobotThread robotThread : list) {
                  funs +=
                      robotThread.currentEvent.getFunctionInfo()
                          + "["
                          + robotThread.currentEvent.getClass().getSimpleName()
                          + "],";
                }

                // Log4jManager.getInstance().error(window,
                // "客户端流程卡住,连续3秒发送量没有变化,当前机器人功能:" + funs + "...");

                Log4jManager.getInstance().error(window, "funs" + funs);
              }
            }

            Runtime runtime = Runtime.getRuntime();
            long freeMem = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            long tMemory = ((totalMemory) / (1024 * 1024));
            long fMemory = ((freeMem) / (1024 * 1024));
            long uMemory = tMemory - fMemory;

            GarbageCollectorInfo collectorInfo = new GarbageCollectorInfo();
            collectorInfo.collectGc();
            long oldGcTime = collectorInfo.getLastOldGcCount();

            String info =
                "totalMemory:["
                    + tMemory
                    + "]MB"
                    + " freeMemory:["
                    + fMemory
                    + "]MB"
                    + " usedMemory:["
                    + uMemory
                    + "]MB oldGC:"
                    + oldGcTime
                    + "\n累积创建:"
                    + window.getCtx().getConnectionTots()
                    + ",当前连接:"
                    + window.getCtx().getNowConnections()
                    + ",当前登陆:"
                    + window.getCtx().getLogined()
                    + ";队列发送:"
                    + sendLogicMsgs
                    + ",实际发送:"
                    + sendMsgs
                    + ",接收数据:"
                    + recvMsgs;

            boolean isStop = false;
            if (window.getConsole().isIncre()) {
              if (lastSendMsgs == 0
                  && (lastSendMsgs == llastSendMsgs)
                  && (llastSendMsgs == sendMsgs)) {
                isStop = true;
              }
            } else {
              if ((lastSendMsgs == llastSendMsgs) && (llastSendMsgs == sendMsgs)) {
                isStop = true;
              }
            }

            if (isStop) {
              List<RobotThread> list = StressRobotManager.instance().getRobots();
              String funs = "";
              for (RobotThread robotThread : list) {
                try {
                  if (robotThread == null) {
                    Log4jManager.getInstance().error(window, "robotThread is null");
                  } else if (robotThread.currentEvent == null) {
                    Log4jManager.getInstance()
                        .error(
                            window,
                            " robotThread.currentEvent is null, isPause:"
                                + FunctionWindow.getInstance().isPause());
                  } else {
                    funs += robotThread.currentEvent.getFunctionInfo() + ",";
                  }
                } catch (Exception e) {
                  Log4jManager.getInstance().error(e);
                }
              }

              // Log4jManager.getInstance().error(window,
              // "客户端流程卡住,连续3秒发送量没有变化,当前机器人功能:" + funs + "...");
            }

            llastSendMsgs = lastSendMsgs;
            lastSendMsgs = sendMsgs;

            Log4jManager.getInstance().info(window, info);
          } catch (Exception e) {
            Log4jManager.getInstance().error(window, e);
          }
        };

    scheduledSinglePoolLog.scheduleWithFixedDelay(r, 0, 1000 * 1, TimeUnit.MILLISECONDS);
  }

  public static void stopLogExecute() {
    scheduledSinglePoolLog.shutdown();
    while (!scheduledSinglePoolLog.isTerminated()) {}
  }

  // ----------------------------------------------------------------
  static ScheduledExecutorService scheduledSinglePoolLog;

  private static final Log4jManager INSTANCE = new Log4jManager();

  private static Logger CLIENT;

  private Log4jManager() {}
}
