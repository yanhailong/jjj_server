package com.jjg.game.logic.robot.event;

import com.jjg.game.core.Log4jManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.jjg.game.logic.robot.entity.RobotPlayer;

/**
 * @author 2CL
 */
public abstract class AbstractEventTypeContainer {

  ERobotEventType eventType;
  /** eventId <=> 事件处理类 */
  protected Map<Integer, AbstractRobotEventHandler> eventMap = new HashMap<>();

  public AbstractEventTypeContainer(ERobotEventType eventType) {
    this.eventType = eventType;
    this.initEvents();
  }

  /** 初始化事件 */
  protected abstract void initEvents();

  public boolean checkTrigger(
      RobotPlayer robotPlayer, RobotEvent event, List<Integer> finishParam) {
    int eventId = event.getEventId();
    if (!eventMap.containsKey(eventId)) {
      Log4jManager.getInstance().error(eventType.name() + " not contain event id " + eventId);
      return false;
    }
    return eventMap.get(eventId).handleCheckTrigger(robotPlayer, event.getParams(), finishParam);
  }
}
