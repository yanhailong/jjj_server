package com.jjg.game;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.FileEx;
import com.jjg.game.utils.LoggerUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.jjg.game.UI.window.frame.FunctionWindow;
import com.jjg.game.UI.window.frame.MessageWindow;
import com.jjg.game.conf.NoneSurfaceRobot;
import com.jjg.game.conf.RunConf;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.core.processor.RobotProcessorManager;
import com.jjg.game.core.robot.StressRobotManager;
import com.jjg.game.utils.MiscUtils;

/**
 * @author 2CL
 * @function 主函数
 */
public class StressToolStarter {

  public static void main(String[] args) throws Exception {
    // 初始化日志系统
    LoggerUtils.init(MiscUtils.isIdeEnvironment());
    // 加载配置表
    loadGameConfigData();
    // 初始化机器人逻辑线程
    RobotProcessorManager.getInstance().init();
    // 初始化事件模板
    EventScanner.initEventClasses();
    // 初始化协议
    try {
      EventScanner.initMessageClasses();
      EventScanner.initRespMessageClasses();
    } catch (Exception e) {
      Log4jManager.getInstance().error(ExceptionEx.e2s(e));
      System.exit(1);
    }
    // 加载工具配置信息
    loadGameStartConfig(args);
  }

  public static void loadGameConfigData() {}

  /**
   * 加载工具启动配置
   *
   * @param args 启动参数
   * @throws IOException e
   */
  public static void loadGameStartConfig(String[] args) throws IOException {
    String openMod = System.getProperty("openMod");
    String os = System.getProperty("os.name");
    // os = RunConf.OS_LINUX;
    if (os.startsWith(RunConf.OS_LINUX)) {
      NoneSurfaceRobot main = new NoneSurfaceRobot();
      if ("config".equals(openMod)) {
        if (args.length > 2) {
          Map<String, String> params = new HashMap<>();
          for (int i = 1; i < args.length; i = i + 2) {
            params.put(args[i], args[i + 1]);
            LoggerUtils.LOGGER.info("修改配置表参数key:{},value:{}", args[i], args[i + 1]);
          }
          modifyConfig(args[0], params);
        }
        main.initFromXml(args[0]);
      } else if ("argv".equals(openMod)) {
        main.initFromArgv(args);
      } else {
        System.exit(1);
      }
      StressRobotManager.instance().start(main);
    } else {
      if ("config".equals(openMod)) {
        if (args.length > 2) {
          Map<String, String> params = new HashMap<>();
          for (int i = 1; i < args.length; i = i + 2) {
            params.put(args[i], args[i + 1]);
          }
          modifyConfig(args[0], params);
        }
        MessageWindow.getInstance().initFromXml(args[0]);
        FunctionWindow.getInstance().initFromXml(args[0]);
      } else if ("argv".equals(openMod)) {
        MessageWindow.getInstance().initFromArgv(args);
        FunctionWindow.getInstance().initFromArgv(args);
      } else {
        boolean openMessage = false;
        if (FileEx.isExists("./config/windowIndex.txt")) {
          String select = FileEx.readAll("./config/windowIndex.txt");
          select = select.trim();
          if ("2".equals(select)) {
            openMessage = true;
          }
        }
        // 这里暂时这样 后面优化
        MessageWindow.getInstance().setVisible(openMessage);
        FunctionWindow.getInstance().setVisible(!openMessage);
        MessageWindow.getInstance().updateChoosedServer();
        FunctionWindow.getInstance().updateChoosedServer();
        FunctionWindow.getInstance().initFunctionMode();
      }
    }
  }

  public static void modifyConfig(String filePath, Map<String, String> params) {
    try {
      File file =
          new File(
              SystemUtils.USER_DIR
                  + File.separator
                  + RunConf.CONFIG_PATH
                  + File.separator
                  + filePath);
      SAXReader saxReader = new SAXReader();
      Document document = saxReader.read(file);
      Element elRoot = document.getRootElement();

      for (Map.Entry<String, String> entry : params.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        if ("robot-name".equals(key)) {
          String sysGroup = System.getProperty("groups");
          if (sysGroup != null && !sysGroup.isEmpty()) {
            String[] split = StringUtils.split(sysGroup, "_");
            int group = Integer.parseInt(split[0]);
            value += "0" + group;
          }
        }

        Element element = elRoot.element(key);
        if (element == null) {
          continue;
        }
        element.setText(value);
      }
      // 格式化为缩进格式
      OutputFormat format = OutputFormat.createPrettyPrint();
      // 设置编码格式
      format.setEncoding("UTF-8");

      XMLWriter writer = new XMLWriter(new FileWriter(file), format);
      // 写入数据
      writer.write(document);
      writer.close();

    } catch (IOException | DocumentException ex) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(ex));
    }
  }
}
