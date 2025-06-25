package com.jjg.game.utils;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.ILoggerFactory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具
 *
 * @author 2CL
 */
public class LoggerUtils {

  private static final String SYSLOG = "asyncsystem";
  public static final String STRUCTURED_LOG = "structuredlogger";

  private static final Lock REENTRANT_LOCK = new ReentrantLock();

  public static Logger LOGGER;
  protected static Logger STRUCTURED_LOGGER;

  static {
    LOGGER = LoggerFactory.getLogger(SYSLOG);
    if (LOGGER == null) {
      LOGGER = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      LOGGER.error("can not find default asyncsystem logger, now use root replace.");
    }
    STRUCTURED_LOGGER = LoggerFactory.getLogger(STRUCTURED_LOG);
    if (STRUCTURED_LOGGER == null) {
      STRUCTURED_LOGGER = LOGGER;
      LOGGER.error(
          "can not find default structuredlogger logger, now use asyncsystem logger replace.");
    }
  }

  protected boolean isDebugEnabled() {
    return LOGGER.isDebugEnabled();
  }

  protected boolean isInfoEnabled() {
    return LOGGER.isInfoEnabled();
  }

  protected boolean isWarnEnabled() {
    return LOGGER.isWarnEnabled();
  }

  protected boolean isErrorEnabled() {
    return LOGGER.isErrorEnabled();
  }

  /**
   * @param logBean 日志对象
   * @param fQcnOfLogger 日志接口调用堆栈终结点类名,即需要输出日志调用点文件名和行号的所在类后的第一个日志工具类
   */
  protected void debug(Object logBean, String fQcnOfLogger) {}

  /**
   * @param logBean 日志对象
   * @param fQcnOfLogger 日志接口调用堆栈终结点类名,即需要输出日志调用点文件名和行号的所在类后的第一个日志工具类
   */
  protected void info(Object logBean, String fQcnOfLogger) {}

  /**
   * @param logBean 日志对象
   * @param fQcnOfLogger 日志接口调用堆栈终结点类名,即需要输出日志调用点文件名和行号的所在类后的第一个日志工具类
   */
  protected void warn(Object logBean, String fQcnOfLogger) {}

  /**
   * @param logBean 日志对象
   * @param fQcnOfLogger 日志接口调用堆栈终结点类名,即需要输出日志调用点文件名和行号的所在类后的第一个日志工具类
   */
  protected void error(Object logBean, String fQcnOfLogger) {}

  public static void init(boolean isDebug) {
    if (isDebug) {
      initDebug();
    } else {
      initOfficial();
    }
  }

  /** 根据是否DEBUG,判断是否重设日志等级 */
  private static void initDebug() {
    setRootLevel(Level.DEBUG);

    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    config.getLoggerConfig(SYSLOG).setLevel(Level.DEBUG);
    if (STRUCTURED_LOGGER != LOGGER) {
      config.getLoggerConfig(STRUCTURED_LOG).setLevel(Level.DEBUG);
    }
  }

  /** 正式环境日志设置 */
  private static void initOfficial() {
    setRootLevel(Level.INFO);
    reMoveConsoleAppender();
  }

  public static Logger getLogger(String name) {
    return LoggerFactory.getLogger(name);
  }

  public static Logger getLogger(Class<?> clazz) {
    return LoggerFactory.getLogger(clazz);
  }

  private static void setRootLevel(Level level) {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    // Check for real log4j2 implementation of slf4j
    if (loggerFactory instanceof Log4jLoggerFactory) {
      LoggerUtils.LOGGER.info("modify log4j2 level to {}", level);
      Configurator.setRootLevel(level);
    } else {
      LoggerUtils.LOGGER.error(
          "unImp log for setRootLevel, current loggerFactory {}", loggerFactory);
    }
  }

  /** 动态修改日志配置文件 */
  private static void reMoveConsoleAppender() {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    // Check for real log4j2 implementation of slf4j
    if (loggerFactory instanceof Log4jLoggerFactory) {
      final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
      // 本类初始化时ctx已经初始化完成,因此首先移除控制台输出再进行监听注册
      reMoveLog4jConsoleAppender(ctx);
      ctx.addPropertyChangeListener(
          evt -> {
            // updateLoggers()与setConfiguration()触发
            if (!Objects.equals(evt.getPropertyName(), LoggerContext.PROPERTY_CONFIG)) {
              return;
            }
            // 一般情况下updateLoggers()前后的config是同一对象,updateLoggers()不需要进行移除appender
            if (evt.getOldValue() == evt.getNewValue()) {
              return;
            }
            // 这里为了处理可能的并发问题
            // Configuration.addListener()后的执行逻辑可能是并发的,当前重载log4j2配置即为并发逻辑
            REENTRANT_LOCK.lock();
            try {
              reMoveLog4jConsoleAppender(ctx);
            } finally {
              REENTRANT_LOCK.unlock();
            }
          });
    } else {
      LoggerUtils.LOGGER.error(
          "unImp log for reMoveRootConsoleAppender,  current loggerFactory {}", loggerFactory);
    }
  }

  private static void reMoveLog4jConsoleAppender(final LoggerContext ctx) {
    final Configuration config = ctx.getConfiguration();

    LoggerUtils.LOGGER.info("begin reMoveRootConsoleAppender log4j2");

    config.getRootLogger().removeAppender("stdoutOut");
    config.getRootLogger().removeAppender("stdoutErr");

    LoggerUtils.LOGGER.info("begin reMoveSYSLOGConsoleAppender log4j2");

    config.getLoggerConfig(SYSLOG).removeAppender("stdoutOut");
    config.getLoggerConfig(SYSLOG).removeAppender("stdoutErr");

    LoggerUtils.LOGGER.info("begin reMoveSTRUCTUREDLOGConsoleAppender log4j2");

    config.getLoggerConfig(STRUCTURED_LOG).removeAppender("stdoutOut");
    config.getLoggerConfig(STRUCTURED_LOG).removeAppender("stdoutErr");
    ctx.updateLoggers();
  }
}
