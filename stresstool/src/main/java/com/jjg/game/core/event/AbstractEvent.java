package com.jjg.game.core.event;

import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.core.robot.StressRobotManager;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.utils.MiscUtils;

/**
 * @author 2CL
 * @function 抽象事件类
 */
public abstract class AbstractEvent<T> implements IEventAction<T> {

  /** 机器人 */
  public final RobotThread robot;

  protected int counter = 0;

  protected int resOrder = -1;

  public AbstractEvent(RobotThread robot) {
    this.robot = robot;
  }

  public AbstractEvent(RobotThread robot, int resOrder) {
    this.robot = robot;
    this.resOrder = resOrder;
  }

  public String getFunctionInfo() {
    FuncTestEvent funcTestEventClazz = this.getClass().getAnnotation(FuncTestEvent.class);
    if (funcTestEventClazz != null) {
      FunctionType functionType = funcTestEventClazz.functionT();
      return functionType.fName;
    } else {
      return "";
    }
  }

  public FunctionType getFunctionType() {
    FuncTestEvent funcTestEventClazz = this.getClass().getAnnotation(FuncTestEvent.class);
    if (funcTestEventClazz != null) {
      FunctionType functionType = funcTestEventClazz.functionT();
      return functionType;
    } else {
      return null;
    }
  }

  /**
   * 执行事件
   *
   * @param msgEntity 消息实体
   * @param objParm 额外参数
   * @param <M> 消息实体泛型
   */
  public final <M> void doAction(M msgEntity, Object... objParm) {
    try {
      if (this.robot == null) {
        Log4jManager.getInstance().error("############Robot Null#############");
      }
      assert this.robot != null;
      boolean isResEnterGameMsg = (!this.robot.getPlayer().getIsLogin()
          /*&& this.resOrder == S2CLoginMsg.ResEnterGame.MsgID.eMsgID_VALUE*/ );
      // 登录成功或者进入游戏请求
      if (this.robot.getPlayer().getIsLogin() || isResEnterGameMsg) {
        counter = 0;
        action((T) msgEntity, objParm);
        if (counter <= 0) {
          String className = getClass().getSimpleName();
          if (className.startsWith("Req")) {
            Log4jManager.getInstance()
                .warn(
                    robot.getWindow(),
                    "############ 功能卡住  强行跳过############" + getClass().getName());
            robotSkipRun();
          }
        }
      }
    } catch (Exception e) {
      Log4jManager.getInstance().error(robot.getWindow(), e);
    }
    String actionResult = actionOver();
    if (actionResult != null && !actionResult.isEmpty()) {
      if (MiscUtils.isIdeEnvironment()
          && robot.getWindow().getRunConf().robotConf.getRobotHoldNum() <= 5
          && robot.getWindow().getRunConf().robotConf.getSendDelayTime() >= 500) {
        Log4jManager.getInstance().info(robot.getWindow(), actionResult);
      }
    }
  }

  /**
   * 慎用
   *
   * <p>仅适用于很特殊的逻辑
   *
   * <p>跳过等待执行间隔,立即执行下一条指令
   */
  public void robotSkipRun() {
    robot.run(true);
    counter++;
  }

  /** 事件执行结束 */
  public String actionOver() {
    return "robot:"
        + robot.getName()
        + ","
        + this.getClass().getName()
        + "'s event action over !"
        + "Thread:"
        + Thread.currentThread().getId();
  }

  /**
   * 慎用
   *
   * <p>发送消息
   *
   * <p>该接口会跳过消息等待,直接进行下一次逻辑执行.
   *
   * @param msg
   * @param isContiue 是否调过等待逻辑直接执行下一调
   */
  protected final void sendMsg(SMessage msg, boolean isContiue) {
    StressRobotManager.instance().addSendMsgPool(robot, msg, isContiue);
    counter++;
  }

  /**
   * 发送简单的应答消息
   *
   * <p>发送消息
   *
   * <p>该接口不影响待测试的请求队列,也不会等待消息返回,只是将消息发送出去
   *
   * @param msg
   */
  protected final void sendResponseMsg(SMessage msg) {
    StressRobotManager.instance().addSendMsgPool(robot, msg, true, false);
    counter++;
  }

  /**
   * 发送消息
   *
   * @param msg
   */
  protected final void sendMsg(SMessage msg) {
    sendMsg(msg, false);
    counter++;
  }

  /**
   * 发送GM指令<br>
   * 注意,禁止调用此方法!<br>
   * 此方法需要服务器GM指令处理逻辑配合 使用此方法,需要服务器GM指令那处理这个方法添加的机器人标记(暂时未处理)
   *
   * @param gmStr
   */
  @Deprecated
  protected final void sendGMCommandMsg(String gmStr) {
    // 发送GM请求
    // 获取事件名字
    String eventName = this.getClass().getName();
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(gmStr);
    stringBuilder.append(" ");
    stringBuilder.append(eventName);
    // 添加机器人请求GM命令的标记
    stringBuilder.append(" robotGM");

    // 组装消息
    /*String finalStr = stringBuilder.toString();
    C2SGmMsg.ReqGmMsg.Builder builder = C2SGmMsg.ReqGmMsg.newBuilder();
    builder.setContent(finalStr);
    SMessage msg =
        new SMessage(
            C2SGmMsg.ReqGmMsg.MsgID.eMsgID_VALUE,
            builder.build().toByteArray(),
            S2CGmMsg.ResGmMsg.MsgID.eMsgID_VALUE);
    // 发送到服务器
    sendMsg(msg);*/
  }
}
