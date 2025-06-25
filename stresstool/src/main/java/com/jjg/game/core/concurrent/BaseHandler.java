package com.jjg.game.core.concurrent;

/**
 * 线程执行handler基类
 *
 * @author 2CL
 */
public abstract class BaseHandler {
  /** 创建handler时间 * */
  protected volatile long time;

  protected volatile long aloneNum;
  protected volatile long createAloneNum;

  /** 执行结果 用于回调类型的handler * */
  protected volatile String result;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  /** handler 执行接口 */
  public abstract void action() throws Exception;

  public void setAloneNum(long l) {
    this.aloneNum = l;
  }

  public long getAloneNum() {
    return this.aloneNum;
  }

  public void setCreateAloneNum(long incrementAndGet) {
    this.createAloneNum = incrementAndGet;
  }

  public long getCreateAloneNum() {
    return this.createAloneNum;
  }
}
