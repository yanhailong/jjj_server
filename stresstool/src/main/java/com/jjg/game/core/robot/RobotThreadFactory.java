package com.jjg.game.core.robot;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.jjg.game.conf.IMainWindow;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.core.event.FuncTestEvent;

/**
 * @function 机器人工厂
 * @author 2CL
 */
public final class RobotThreadFactory {
  private RobotThreadFactory() {}

  public static volatile AtomicLong counter = new AtomicLong(0);

  /** 压测机器人池 */
  private static final Map<String, RobotThread> STRESS_ROBOT_POOL = new ConcurrentHashMap<>();

  /** 初始化机器人 去掉synchronized关键词,继续查看数据问题 */
  public static RobotThread createNewRobot(IMainWindow window, boolean isSingle) {
    String name = window.getRunConf().robotConf.getPrefixName();
    if (!isSingle) {
      // 轮询最大用户数进行迭代
      if (window.getRunConf().robotConf.getRobotMaxNum() != -1
          && counter.get() >= window.getRunConf().robotConf.getRobotMaxNum()) {
        counter.set(0);
      }

      counter.incrementAndGet();
      name += "000" + counter;
    }

    RobotThread rtd = new RobotThread(name, counter.get(), window);
    loadEvents(rtd);
    long now = System.currentTimeMillis();
    rtd.setCreateTime(now);
    return rtd;
  }

  /** 存入机器人池 */
  public static void putRobot(String channelId, RobotThread cRobotThread) {
    // synchronized (robotsMap) {
    STRESS_ROBOT_POOL.put(channelId, cRobotThread);
    // }
  }

  /** 存入机器人池 */
  public static RobotThread removeRobot(String key) {
    return STRESS_ROBOT_POOL.remove(key);
  }

  /** 为机器人加载事件 */
  private static void loadEvents(RobotThread crt) {
    try {
      crt.initEvents(
          constructEventsMap(EventScanner.getRequestOnceEventClasses(), crt, true),
          constructEventsMap(EventScanner.getResponseEventClasses(), crt, false),
          constructEventsMap(EventScanner.getSelectedRequestMultipleEventClasses(), crt),
          constructEventsMap(EventScanner.getSelectedRequestOnceEventClasses(), crt));
    } catch (Exception e) {
      Log4jManager.getInstance().error(crt.getWindow(), e);
    }
  }

  /** 获取机器人线程 */
  public static RobotThread getRobot(String channelId) {
    return STRESS_ROBOT_POOL.get(channelId);
  }

  /** 随机取出一个robot */
  public static Map<String, RobotThread> getAllRobots() {
    return STRESS_ROBOT_POOL;
  }

  /** 构造单次请求以及响应事件MAP */
  private static Map<Integer, AbstractEvent<?>> constructEventsMap(
      List<Class<? extends AbstractEvent<?>>> classes, RobotThread crt, boolean isOrder)
      throws Exception {
    Map<Integer, AbstractEvent<?>> eventMap = isOrder ? new TreeMap<>() : new HashMap<>();
    FuncTestEvent funcTestEvent;
    for (Class<? extends AbstractEvent<?>> clazz : classes) {
      funcTestEvent = clazz.getAnnotation(FuncTestEvent.class);
      if (null != funcTestEvent) {
        Constructor<? extends AbstractEvent<?>> constructor =
            clazz.getConstructor(RobotThread.class);
        AbstractEvent<?> event = constructor.newInstance(crt);
        eventMap.put(funcTestEvent.order(), event);
      }
    }
    return eventMap;
  }

  /** 构建可重复请求事件MAP */
  private static Map<FunctionType, Map<Integer, AbstractEvent<?>>> constructEventsMap(
      Map<FunctionType, List<Class<? extends AbstractEvent<?>>>> clazzMap, RobotThread crt) {
    Iterator<Entry<FunctionType, List<Class<? extends AbstractEvent<?>>>>> iterator =
        clazzMap.entrySet().iterator();
    Map<FunctionType, Map<Integer, AbstractEvent<?>>> eventsMap = new HashMap<>();
    iterator.forEachRemaining(
        action -> {
          try {
            Map<Integer, AbstractEvent<?>> eventMap =
                constructEventsMap(action.getValue(), crt, true);
            eventsMap.put(action.getKey(), eventMap);
          } catch (Exception e) {
            Log4jManager.getInstance().error(crt.getWindow(), e);
          }
        });
    return eventsMap;
  }
}
