package com.jjg.game.logic.robot.event.container;

import java.util.List;
import java.util.Map;
import com.jjg.game.logic.robot.entity.RobotPlayer;
import com.jjg.game.logic.robot.event.AbstractEventTypeContainer;
import com.jjg.game.logic.robot.event.AbstractRobotEventHandler;
import com.jjg.game.logic.robot.event.ERobotEventType;

/**
 * @author 2CL
 */
public class RobotPlayerEventContainer extends AbstractEventTypeContainer {

  public static final int PLAYER_LEVEL = 1001;

  public RobotPlayerEventContainer() {
    super(ERobotEventType.PLAYER);
  }

  @Override
  protected void initEvents() {
    RobotPlayerLevelEventHandler playerLevelEvent = new RobotPlayerLevelEventHandler();
    eventMap.put(PLAYER_LEVEL, playerLevelEvent);
  }

  /** 机器人等级 */
  private static class RobotPlayerLevelEventHandler extends AbstractRobotEventHandler {

    protected RobotPlayerLevelEventHandler() {
      super(PLAYER_LEVEL);
    }

    @Override
    protected boolean checkTrigger(
        RobotPlayer player, Map<String, Object> param, List<Integer> finishParams, int target) {
      return /*player.getPlayerInfo().getLvl() >= target*/true;
    }
  }
}
