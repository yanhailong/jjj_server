package com.jjg.game.logic.robot.event;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * @author 2CL
 */
public class RobotEvent {

  private ERobotEventType eventType;
  private int eventId;
  private Map<String, Object> params;

  public RobotEvent(ERobotEventType eventType, int eventId) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.params = Maps.newHashMap();
  }

  public RobotEvent(ERobotEventType eventType, int eventId, Map<String, Object> params) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.params = params;
  }

  public ERobotEventType getEventType() {
    return eventType;
  }

  public int getEventId() {
    return eventId;
  }

  public Map<String, Object> getParams() {
    return params;
  }
}
