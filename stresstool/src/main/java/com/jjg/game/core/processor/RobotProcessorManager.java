package com.jjg.game.core.processor;

import java.util.HashMap;
import java.util.Map;

import com.jjg.game.core.concurrent.BaseHandler;
import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.LoggerUtils;

public class RobotProcessorManager {
  /** 逻辑线处理器集合 */
  private final Map<Integer, RobotProcessor> robotProcessors = new HashMap<>();

  private int processNum;

  /**
   * 初始化机器人logic处理线程数量 cpu处理器核心数-2
   *
   * @throws Exception e
   */
  public void init() throws Exception {
    this.processNum = countProcessNum();
    LoggerUtils.LOGGER.info("init robot processor ,created {} robotProcessor", this.processNum);
    for (int i = 0; i < this.processNum; i++) {
      RobotProcessor processor = new RobotProcessor(i);
      robotProcessors.put(i, processor);
    }
  }

  /**
   * 根据逻辑线程Id 获取处理线程
   *
   * @param lineId
   * @return
   */
  public RobotProcessor getProcessor(int lineId) {
    return robotProcessors.get(lineId);
  }

  public void addRobotHandlerByPlayerId(long playerId, BaseHandler handler) {
    // playerid是固定的,processNum也是固定的,所以每次得到的line也是固定的
    int line = Math.toIntExact(playerId % this.processNum);
    addRobotHandler(line, handler);
  }

  /**
   * 向指定的机器人线程抛送handler
   *
   * @param line
   * @param gameInnerHandler
   */
  public void addRobotHandler(int line, BaseHandler handler) {
    RobotProcessor processor = getProcessor(line);
    if (processor == null) {
      LoggerUtils.LOGGER.error(
          "Add Command to RobotProcessor . Can Not Find Processor. index :"
              + line
              + ExceptionEx.currentThreadTraces());
      return;
    }
    processor.executeHandler(handler);
  }

  public static int countProcessNum() {
    int processNum = 1;
    if (Runtime.getRuntime().availableProcessors() > 5) {
      processNum = Runtime.getRuntime().availableProcessors() - 2;
    } else {
      processNum = Runtime.getRuntime().availableProcessors() - 1;
    }
    if (processNum <= 0) {
      processNum = 1;
    }

    return processNum;
  }

  private enum Singleton {
    /** 单例 */
    INSTANCE;
    RobotProcessorManager manager;

    Singleton() {
      this.manager = new RobotProcessorManager();
    }

    RobotProcessorManager getManager() {
      return manager;
    }
  }

  public static RobotProcessorManager getInstance() {
    return Singleton.INSTANCE.getManager();
  }
}
