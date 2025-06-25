package com.jjg.game.logic.robot.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.jjg.game.logic.robot.entity.RobotPlayer;
import org.apache.commons.lang3.StringUtils;

/**
 * @author 2CL
 */
public class RobotEventUtils {

  public static boolean isTriggerCompletely(
      RobotPlayer player, String finishParamCfg, Map<Integer, Integer> eventCfgIds) {
    // 模拟任务系统接取任务事件检测
    String[] finishParamArray = StringUtils.split(finishParamCfg, '|');
    boolean complete = false;
    for (int i = 1; i <= eventCfgIds.size(); i++) {
      int eventCfgId = eventCfgIds.get(i);
      /*TriggerTypeCfgBean triggerTypeCfgBean = GameDataManager.getTriggerTypeCfgBean(eventCfgId);
      if (triggerTypeCfgBean == null) {
        continue;
      }*/
      int type = /*triggerTypeCfgBean.getType()*/ 1;
      ERobotEventType eventType = ERobotEventType.getEventType(type);
      // 触发该事件
      String[] finishParamDetail = StringUtils.split(finishParamArray[i - 1], ',');
      List<Integer> taskFinishParam = new ArrayList<>();
      for (String finishParam : finishParamDetail) {
        taskFinishParam.add(Integer.parseInt(finishParam));
      }
      // 当前系统的事件检测必须是玩家自身有存储的数据而非需要传递参数的检测,否则无法正确进行触发
      complete =
          ERobotEventType.getEventType(type)
              .getAbstractEventContainer()
              .checkTrigger(player, new RobotEvent(eventType, eventCfgId, null), taskFinishParam);
    }
    return complete;
  }
}
