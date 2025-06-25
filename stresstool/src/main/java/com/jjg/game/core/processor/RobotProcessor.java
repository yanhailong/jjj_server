package com.jjg.game.core.processor;

import com.jjg.game.core.concurrent.BaseHandler;
import com.jjg.game.core.concurrent.BaseProcessor;
import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.LoggerUtils;

public class RobotProcessor extends BaseProcessor {
  /** 逻辑线index */
  private final int lineIndex;

  public RobotProcessor(int lineIdex) {
    super("RobotProcessor_" + lineIdex, 1);
    this.lineIndex = lineIdex;
  }

  @Override
  public void executeHandler(BaseHandler handler) {
    try {
      super.executeHandler(handler);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
  }
}