package com.jjg.game.conf;

import com.jjg.game.core.Log4jManager;
import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.FileEx;
import com.jjg.game.utils.LoggerUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** @function 运行配置 */
public final class RunConf {

  public static final String CONFIG_PATH = "config";

  private static final SAXReader READER = new SAXReader();

  public static final String OS_WINDOWS = "Windows";
  public static final String OS_LINUX = "Linux";

  public static boolean pause = false;

  public boolean init = false;

  /** 本地登录服连接地址 */
  public String localLoginUrl;

  /** 内网登录服连接地址 */
  public String devLoginUrl;

  /** 当前选择的服务器配置 */
  public ServerConf choosedServer;

  /** 当前机器人配置 */
  public RobotConf robotConf = new RobotConf();

  /** 服务器配置列表 */
  public final List<ServerConf> SERVERCONFS = new ArrayList<ServerConf>();

  public List<String> funcGroups;

  public RunConf() {
    try {
      Log4jManager.getInstance().init();
      loadServersConf();
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      System.exit(1);
    }
  }

  public void init(String serverGroup) {
    try {
      init = true;
      // 从配置文件读取
      String from = null;
      String server = null;

      String s = serverGroup;
      if (s == null) {
        if (FileEx.isExists("./config/serverIndex.txt")) {
          String select = FileEx.readAll("./config/serverIndex.txt");
          select = select.trim();
          if (select != null && !select.isEmpty()) {
            String[] split = select.split("_");
            if (split.length >= 2) {
              from = split[0];
              server = split[1];
            }
          }
        }
      } else { // 直接传参过来
        String[] split = StringUtils.split(s, "@");
        from = split[0];
        server = split[1];
      }

      if (from != null && server != null) {
        for (ServerConf sc : SERVERCONFS) {
          if (sc.getType().equals(from) && sc.getName().equals(server)) {
            choosedServer = sc;
            break;
          }
        }
      }
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      System.exit(1);
    }
  }

  public boolean loadRobotInfo() {
    try {
      // 从配置文件读取
      if (FileEx.isExists("./config/robotInfo.txt")) {
        List<String> select = FileEx.readLines("./config/robotInfo.txt");
        if (!select.isEmpty()) {
          int maxRobotNum = Integer.parseInt(select.get(0));
          int holdNum = Integer.parseInt(select.get(1));
          int delayTime = Integer.parseInt(select.get(2));
          String robotName = select.get(3);
          boolean isSingle = false;
          if (select.size() > 4) {
            isSingle = (Integer.parseInt(select.get(4)) == 1);
          }

          robotConf.setRobotMaxNum(maxRobotNum);
          robotConf.setRobotHoldNum(holdNum);
          robotConf.setSendDelayTime(delayTime);
          robotConf.setPrefixName(robotName);
          robotConf.setSingle(isSingle);
          return true;
        }
      }
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      System.exit(1);
    }
    return false;
  }

  public RobotConf getRobotConf() {
    return this.robotConf;
  }

  /** 加载服务器配置 */
  private void loadServersConf() throws Exception {
    File serverConf =
        new File(
            SystemUtils.USER_DIR
                + File.separator
                + CONFIG_PATH
                + File.separator
                + "serverConf.xml");
    Document document = READER.read(serverConf);
    Element elRoot = document.getRootElement();
    List<Element> list = elRoot.elements();
    for (Element e : list) {
      if (e.getName().equals("servers")) {
        e.elements()
            .forEach(
                ee ->
                    SERVERCONFS.add(
                        new ServerConf(
                            Integer.parseInt(ee.element("serverPort").getStringValue()),
                            ee.element("serverIp").getStringValue(),
                            ee.element("type").getStringValue(),
                            ee.element("name").getStringValue())));
      } else if ("loginUrl".equals(e.getName())) {
        localLoginUrl = e.element("localLoginUrl").getStringValue();
        devLoginUrl = e.element("devLoginUrl").getStringValue();
      }
    }
  }

  //  /**
  //   * 加载log4j配置
  //   *
  //   * @throws Exception
  //   */
  //  public static void loadLog4jConf() throws Exception {
  //    //    StringBuilder pathBuilder =
  //    //        new StringBuilder(
  //    //            System.getProperty("user.dir") + File.separator + CONFIG_PATH +
  // File.separator);
  //    //    if (MiscUtils.isIDEEnvironment()) {
  //    //      pathBuilder.append("log4j_devel.xml");
  //    //    } else {
  //    //      pathBuilder.append("log4j_release.xml");
  //    //    }
  //    //    DOMConfigurator.configureAndWatch(pathBuilder.toString());
  //
  //    //    LoggerUtils.init(MiscUtils.isIDEEnvironment());
  //  }
}
