package com.jjg.game.logic.robot.event;

import com.jjg.game.core.Log4jManager;
import java.util.List;
import java.util.Map;
import com.jjg.game.logic.robot.entity.RobotPlayer;
import com.jjg.game.utils.ExceptionEx;

/**
 * @author 2CL
 */
public abstract class AbstractRobotEventHandler {

  protected int eventId;

  public AbstractRobotEventHandler(int eventId) {
    this.eventId = eventId;
  }

  /**
   * 事件触发
   *
   * @param player 机器人
   * @param param 触发参数 为空时用当前进度进行一次判断
   * @param finishParam 完成参数
   * @return 是否触发成功
   */
  public boolean handleCheckTrigger(
      RobotPlayer player, Map<String, Object> param, List<Integer> finishParam) {
    // 事件目标参数
    /*TriggerTypeCfgBean triggerTypeCfgBean = GameDataManager.getTriggerTypeCfgBean(eventId);
    if (triggerTypeCfgBean == null) {
      Log4jManager.getInstance().error("TriggerType.xlsx does not exist id:" + eventId);
      return false;
    }*/
    int target = 0;
    int count = 100/*triggerTypeCfgBean.getCount()*/;
    try {
      target = finishParam.get(count - 1);
    } catch (Exception e) {
      Log4jManager.getInstance()
          .error(String.format("事件:%d,完成条件:%d,配置异常", eventId, count) + ExceptionEx.e2s(e));
    }
    return checkTrigger(player, param, finishParam, target);
  }

  /**
   * 检查是否触发条件
   *
   * @param player 机器人
   * @param param 触发参数
   * @param target 目标值
   * @param finishParams 完成参数
   * @return 是否触发条件
   */
  protected abstract boolean checkTrigger(
      RobotPlayer player, Map<String, Object> param, List<Integer> finishParams, int target);
}
