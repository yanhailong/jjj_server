package com.jjg.game.conf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.jjg.game.utils.ExceptionEx;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.core.event.FunctionType;

/**
 * @author 2CL
 */
public class NoneSurfaceRobot implements IMainWindow, IConsole {

  private RunConf runConf;

  // 控制台debug开关
  private boolean _debug;

  // 控制台滚屏开关
  private boolean _scroll;

  // 控制台增量开关
  private boolean _incre;

  // 控制台排队登录开关
  private boolean _LoginQue;

  private boolean running = false;

  private RunContext ctx = new RunContext();

  // 压测类型
  private int yaceType;
  // 是否强制无限次
  private boolean isUnLimitTimes;
  // 所有功能执行的最小轮数
  private int minRoundNumber = 1;
  // 所有功能执行的最大轮数
  private int maxRoundNumber = 1;

  @Override
  public RunConf getRunConf() {
    return this.runConf;
  }

  @Override
  public boolean isDebug() {
    return _debug;
  }

  @Override
  public void setDebug(boolean _debug) {
    this._debug = _debug;
  }

  @Override
  public boolean isScroll() {
    return _scroll;
  }

  @Override
  public void setScroll(boolean _scroll) {
    this._scroll = _scroll;
  }

  @Override
  public boolean isIncre() {
    return _incre;
  }

  @Override
  public void setIncre(boolean _incre) {
    this._incre = _incre;
  }

  @Override
  public boolean isLoginQue() {
    return _LoginQue;
  }

  @Override
  public void setLoginQue(boolean _LoginQue) {
    this._LoginQue = _LoginQue;
  }

  @Override
  public void addConsoleAreaInfo(String actionMsg, boolean isDebug) {}

  @Override
  public IConsole getConsole() {
    return this;
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public RunContext getCtx() {
    return this.ctx;
  }

  @Override
  public void initRobot() {
    this.ctx.setAlltoZero();
  }

  @Override
  public void setRunning(boolean running) {
    this.running = running;
  }

  @Override
  public void onStart() {}

  @Override
  public boolean isLocalLogin() {
    return false;
  }

  @Override
  public void onStop() {}

  @Override
  public int getStressTestType() {
    return this.yaceType;
  }

  @Override
  public String getMessageId() {
    return null;
  }

  @Override
  public String getMessageInfo() {
    return null;
  }

  @Override
  public boolean isPause() {
    return false;
  }

  @Override
  public boolean isUnLimitTimes() {
    return isUnLimitTimes;
  }

  /** 初始化功能模块选项 */
  private void initSelectFuncs() {
    String sys_group = System.getProperty("groups");
    int group = -1;
    int max_group = -1;
    if (sys_group != null && !"".equals(sys_group)) {
      String[] split = StringUtils.split(sys_group, "_");
      group = Integer.parseInt(split[0]);
      max_group = Integer.parseInt(split[1]);
    }

    // 之前的算法
    //    int rc = FunctionType.FunCount.realCount;
    //    int st_count = group == 1 ? 0 : (rc / max_group) * (group - 1) + rc % max_group;
    //    int end_count = st_count + rc / max_group + (group == 1 ? rc % max_group : 0) - 1;

    Map<FunctionType, List<Class<? extends AbstractEvent<?>>>> multipleEventmap =
        EventScanner.getRequestMultipleEventClasses();
    Iterator<FunctionType> multipleIterator = multipleEventmap.keySet().iterator();

    List<String> selects = new ArrayList<String>();
    List<FunctionType> readyFunction = new ArrayList<>();
    while (multipleIterator.hasNext()) {
      FunctionType item = multipleIterator.next();
      String name = item.fName;

      if (name == null
          || "".equals(name)
          || name.contains("心跳")
          || name.contains(RobotConstDefine.NO_CHOOSE_FUNCTION)) {
        continue;
      }

      readyFunction.add(item);
    }

    if (group == -1 || max_group == -1) {
      for (FunctionType function : readyFunction) {
        String name = function.fName;
        // 可重复执行的模块显示设定的次数
        if (function.fNum > 0) {
          name += "_" + function.fNum + "次";
        } else {
          name += "_无限次";
        }
        selects.add(name);
      }

    } else {
      int funLenght = readyFunction.size();
      // 选择的功能长度,每组5个功能
      int selectFunLength = 5;
      int st_count = ((group - 1) * selectFunLength) % funLenght;
      for (int i = 0; i < selectFunLength; i++) {
        int index = st_count + i;
        if (index >= funLenght) {
          index = index % funLenght;
        }
        FunctionType item = readyFunction.get(index);

        String name = item.fName;
        // 可重复执行的模块显示设定的次数
        if (item.fNum > 0) {
          name += "_" + item.fNum + "次";
        } else {
          name += "_无限次";
        }

        selects.add(name);
      }

      FunctionType item = FunctionType.HEART_BEAT;
      String name = item.fName;
      if (item.fNum > 0) {
        name += "_" + item.fNum + "次";
      } else {
        name += "_无限次";
      }
      selects.add(name);
    }

    EventScanner.setFunctionMultpleEvents(selects);
    Log4jManager.getInstance().warn("当前选择的功能:" + Arrays.toString(selects.toArray()));
    this.runConf.funcGroups = selects;
  }

  private void initSelectFuncs(List<String> fNames) {
    EventScanner.setFunctionMultpleEvents(fNames);
    this.runConf.funcGroups = fNames;
  }

  @Override
  public void initFromArgv(String[] argv) {
    try {
      System.out.println("server start argv == " + argv);
      // 压测类型 1msg 2func
      yaceType = Integer.parseInt(argv[0]);
      // 格式 Server_2CL || Group_week
      runConf = new RunConf();
      runConf.init(argv[1]);
      runConf.robotConf.setRobotMaxNum(Integer.parseInt(argv[2]));
      runConf.robotConf.setRobotHoldNum(Integer.parseInt(argv[3]));
      runConf.robotConf.setSendDelayTime(1000);
      runConf.robotConf.setPrefixName(argv[4]);
      runConf.robotConf.single = false;
      // 开关 格式 1_1_1_1_1
      if (argv.length >= 6) {
        String[] split = StringUtils.split(argv[5], "_");
        _debug = "1".equals(split[0]);
        _scroll = "1".equals(split[1]);
        _incre = "1".equals(split[2]);
        _LoginQue = "1".equals(split[3]);
      } else {
        _debug = false;
        _scroll = false;
        _incre = false;
        _LoginQue = true;
      }
      // 初始化测试功能
      if (yaceType == 2) {
        initSelectFuncs();
      }
    } catch (Exception e) {
      Log4jManager.getInstance().error(ExceptionEx.e2s(e));
      System.exit(1);
    }
  }

  @Override
  public void initFromXml(String path) {
    try {
      File file =
          new File(
              SystemUtils.USER_DIR + File.separator + RunConf.CONFIG_PATH + File.separator + path);
      SAXReader READER = new SAXReader();
      Document document = READER.read(file);
      Element elRoot = document.getRootElement();

      Element e = elRoot.element("yace-type");
      yaceType = e != null ? Integer.parseInt(e.getStringValue()) : 2;

      e = elRoot.element("server-group");
      String serverGroup = e != null ? e.getStringValue() : "Group_week";

      runConf = new RunConf();
      runConf.init(serverGroup);
      runConf.robotConf.single = false;

      e = elRoot.element("max-robot");
      runConf.robotConf.setRobotMaxNum(e != null ? Integer.parseInt(e.getStringValue()) : 1000);

      e = elRoot.element("max-conn");
      runConf.robotConf.setRobotHoldNum(e != null ? Integer.parseInt(e.getStringValue()) : 50);

      e = elRoot.element("send-delay");
      runConf.robotConf.setSendDelayTime(e != null ? Integer.parseInt(e.getStringValue()) : 1000);

      e = elRoot.element("robot-name");
      runConf.robotConf.setPrefixName(e.getStringValue());

      e = elRoot.element("debug");
      _debug = e != null ? Boolean.parseBoolean(e.getStringValue()) : false;

      e = elRoot.element("scroll");
      _scroll = e != null ? Boolean.parseBoolean(e.getStringValue()) : false;

      e = elRoot.element("incre");
      _incre = e != null ? Boolean.parseBoolean(e.getStringValue()) : false;

      e = elRoot.element("login-queue");
      _LoginQue = e != null ? Boolean.parseBoolean(e.getStringValue()) : true;

      e = elRoot.element("is-unlimit");
      isUnLimitTimes = e != null ? Boolean.parseBoolean(e.getStringValue()) : false;

      e = elRoot.element("min-round");
      minRoundNumber = e != null ? Integer.parseInt(e.getStringValue()) : 1;

      e = elRoot.element("max-round");
      maxRoundNumber = e != null ? Integer.parseInt(e.getStringValue()) : 1;

      if (minRoundNumber > maxRoundNumber) {
        minRoundNumber = 1;
        maxRoundNumber = 1;
      }

      // 初始化测试功能
      if (yaceType == 2) {
        initSelectFuncs();
      }
    } catch (DocumentException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public String getSelectGroup() {
    return runConf.choosedServer.getType();
  }

  @Override
  public int getMinRoundNumber() {
    return minRoundNumber;
  }

  @Override
  public int getMaxRoundNumber() {
    return maxRoundNumber;
  }
}
