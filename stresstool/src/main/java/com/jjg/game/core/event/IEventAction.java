package com.jjg.game.core.event;

/**
 * @author 2CL
 * @function 事件执行接口
 */
public interface IEventAction<T> {

  /**
   * 执行事件
   *
   * @param msgEntity 请求的消息实体类
   * @param obj 额外参数
   * @throws Exception e
   */
  void action(T msgEntity, Object... obj) throws Exception;
}
