package com.jjg.game.core.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 2CL
 * @function 功能压测事件注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FuncTestEvent {
  /** 事件类型 * */
  EventType eventT();

  /** 功能类型(只有当事件类型为重复请求类型时，才加入此参数，否则为空) * */
  FunctionType functionT() default FunctionType.NULL;

  /** 事件编号 请求事件order为请求顺序 返回事件order为返回消息的msgid * */
  int order();
}
