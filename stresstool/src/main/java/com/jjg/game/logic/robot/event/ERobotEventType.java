package com.jjg.game.logic.robot.event;

import com.jjg.game.logic.robot.event.container.RobotPlayerEventContainer;

/**
 * @author 2CL
 */
public enum ERobotEventType {

  /** 玩家事件 */
  PLAYER(1, new RobotPlayerEventContainer()),
  ;

  final int type;
  final AbstractEventTypeContainer abstractEventContainer;

  ERobotEventType(int type, AbstractEventTypeContainer abstractEventTypeContainer) {
    this.type = type;
    this.abstractEventContainer = abstractEventTypeContainer;
  }

  public int getType() {
    return type;
  }

  public AbstractEventTypeContainer getAbstractEventContainer() {
    return abstractEventContainer;
  }

  public static ERobotEventType getEventType(int type) {
    for (ERobotEventType eventType : ERobotEventType.values()) {
      if (eventType.type == type) {
        return eventType;
      }
    }
    throw new IllegalArgumentException("[配置表错误]undefine event type : " + type);
  }
}
