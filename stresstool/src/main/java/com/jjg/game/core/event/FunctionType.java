package com.jjg.game.core.event;

import com.jjg.game.conf.RobotConstDefine;
import com.jjg.game.conf.RunConf;
import com.jjg.game.utils.RandomUtil;

/**
 * @author 2CL
 * @function 功能类型枚举 所有的可重复执行的功能都用此枚举
 */
public enum FunctionType {

  /**
   * 部分功能处理完以后不会再被运行.
   *
   * <p>比如某些功能运行次数为10,并且在逻辑中会执行删除 --> robot.removeCurrentFun();
   *
   * <p>ConstDefine.NO_CHOOSE_FUNCTION 标记则不会在功能选择中显示出来 形如:CHASM
   */
  /** 空功能类型，单次请求事件,说明见{@link EventType#REQUEST_ONCE}和响应事件调用此选项 * */
  NULL(),

  HEART_BEAT("心跳", 1),

  DOLLAR_EXPRESS("美元快递", 5),

  HALL("大厅", 5),
  ;

  FunctionType() {}

  FunctionType(String fName, int funNum) {
    this.fName = fName;
    this.fNum = funNum;
    String os = System.getProperty("os.name");
    // 由于发送间隔在linux上普遍被设置为3-5秒了,功能增多后,跑完一个流程时间太长,减少功能循环次数,方便测试上下线逻辑
    if (os.startsWith(RunConf.OS_LINUX)) {
      this.fNum = RandomUtil.nextIntInclude(1, this.fNum);
    }
    this.index = ++FunCount.totCount;
  }

  FunctionType(String fName, int funNum, boolean required) {
    this.fName = fName;
    this.fNum = funNum;
    String os = System.getProperty("os.name");
    // 由于发送间隔在linux上普遍被设置为3-5秒了,功能增多后,跑完一个流程时间太长,减少功能循环次数,方便测试上下线逻辑
    if (os.startsWith(RunConf.OS_LINUX)) {
      this.fNum = RandomUtil.nextIntInclude(1, this.fNum);
    }
    this.index = ++FunCount.totCount;
    this.required = required;
    this.fName += "(必选)";
    if (!this.required) {
      FunCount.realCount++;
    }
  }

  /** 功能名称(用于UI显示) */
  public String fName;

  /** 功能可执行次数（默认10 0为不限制） */
  public int fNum = 10;

  /** 功能索引 */
  public int index = 0;

  /** 是否是必选功能，游戏核心功能数据 */
  public boolean required;

  public static class FunCount {

    public static int totCount = 0;
    public static int realCount = 0;
  }
}
