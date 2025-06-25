package com.jjg.game.core.event;

/**
 * @author 2CL
 * @function 事件类型枚举
 */
public enum EventType {
  NOT_DEFINED,
  /** 只可请求一次的事件 (前提流程,例如：登录流程或者登录时需要初始化的一次性数据) * */
  REQUEST_ONCE,
  /** 可请求多次的事件 * */
  REQUEST_REPEAT,
  /** 响应事件 * */
  RESPONSE,
}
