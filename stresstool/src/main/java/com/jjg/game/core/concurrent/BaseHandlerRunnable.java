package com.jjg.game.core.concurrent;

/**
 * BaseHandlerRunnable
 *
 * @author 2CL
 */
public abstract class BaseHandlerRunnable implements Runnable {

  protected BaseHandler handler;

  public BaseHandler getHandler() {
    return handler;
  }

  public BaseHandlerRunnable setHandler(BaseHandler handler) {
    this.handler = handler;
    return this;
  }
}
