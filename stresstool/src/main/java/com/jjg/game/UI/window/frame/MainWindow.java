package com.jjg.game.UI.window.frame;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;

import com.jjg.game.utils.ExceptionEx;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.jjg.game.UI.Listener.GmToolsListener;
import com.jjg.game.UI.Listener.HttpToolsListener;
import com.jjg.game.UI.Listener.TestMenuListener;
import com.jjg.game.UI.window.panel.ConsolePanel;
import com.jjg.game.UI.window.panel.RobotPanel;
import com.jjg.game.UI.window.panel.ServerPanel;
import com.jjg.game.conf.IConsole;
import com.jjg.game.conf.IMainWindow;
import com.jjg.game.conf.RunConf;
import com.jjg.game.conf.RunContext;
import com.jjg.game.core.Log4jManager;

public abstract class MainWindow implements IMainWindow {

  public MainWindow() {
    frame = new JFrame();

    JMenuBar menuBar = new JMenuBar();
    menuBar.setBounds(0, 0, 1220, 21);
    frame.getContentPane().add(menuBar);

    JMenu testTypeMenu = new JMenu("测试类型");
    testTypeMenu.setFont(new Font("宋体", Font.PLAIN, 12));
    menuBar.add(testTypeMenu);

    JMenuItem functionMenuItem = new JMenuItem("功能测试");
    functionMenuItem.setFont(new Font("宋体", Font.PLAIN, 12));
    functionMenuItem.addActionListener(new TestMenuListener(frame));
    testTypeMenu.add(functionMenuItem);

    JMenuItem messageMenuItem = new JMenuItem("消息测试");
    messageMenuItem.setFont(new Font("宋体", Font.PLAIN, 12));
    messageMenuItem.addActionListener(new TestMenuListener(frame));
    testTypeMenu.add(messageMenuItem);

    JMenu toolsMenu = new JMenu("工具");
    toolsMenu.setFont(new Font("宋体", Font.PLAIN, 12));
    menuBar.add(toolsMenu);

    JMenuItem gmMenuItem = new JMenuItem("GM工具");
    gmMenuItem.setFont(new Font("宋体", Font.PLAIN, 12));
    gmMenuItem.addActionListener(new GmToolsListener(frame));
    toolsMenu.add(gmMenuItem);

    JMenuItem httpMenuItem = new JMenuItem("HTTP功能开关工具");
    httpMenuItem.setFont(new Font("宋体", Font.PLAIN, 12));
    httpMenuItem.addActionListener(new HttpToolsListener(frame));
    toolsMenu.add(httpMenuItem);

    topChose = new JCheckBox("是否置顶");
    topChose.setSelected(true);
    topChose.setBounds(10, 29, 87, 23);
    topChose.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent arg0) {
            frame.setAlwaysOnTop(topChose.isSelected());
          }
        });

    frame.add(topChose);

    ButtonGroup bg = new ButtonGroup();
    devRadioButton = new JRadioButton("内网登录服",true);
    localRadioButton = new JRadioButton("本地登录服");
    devRadioButton.setBounds(10, 50, 100, 60);
    localRadioButton.setBounds(120, 50, 100, 60);
    bg.add(devRadioButton);
    bg.add(localRadioButton);
    frame.add(devRadioButton);
    frame.add(localRadioButton);

    /*devRadioButton.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (devRadioButton.isSelected()) {
          System.out.println("aaa");
        }
//          FileEx.writeAll("./config/.txt", server);
      }
    });

    localRadioButton.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (localRadioButton.isSelected()) {
          System.out.println("bbb");
        }
      }
    });*/

    runButton = new JButton("开始运行");
    runButton.setName("runbutton");
    runButton.setFont(new Font("宋体", Font.PLAIN, 12));
    runButton.setBounds(27, 496, 120, 23);
    frame.getContentPane().add(runButton);

    consolePanel = new ConsolePanel();
    consolePanel.setLocation(600, 113);
    frame.getContentPane().add(consolePanel);

    serverPanel = new ServerPanel(this);
    serverPanel.setLocation(600, 31);
    frame.getContentPane().add(serverPanel);

    robotPanel = new RobotPanel(this);
    robotPanel.setLocation(8, 112);
    frame.getContentPane().add(robotPanel);
  }

  public void setVisible(boolean isVisible) {
    frame.setVisible(isVisible);
  }

  public RobotPanel getRobotPanel() {
    return robotPanel;
  }

  public JButton getRunButton() {
    return runButton;
  }

  @Override
  public boolean isLocalLogin() {
    return localRadioButton.isSelected();
  }

  public void serBeginStart() {}

  @Override
  public IConsole getConsole() {
    return this.consolePanel;
  }

  @Override
  public boolean isRunning() {
    return !this.runButton.getText().equals("开始运行");
  }

  @Override
  public void setRunning(boolean running) {
    this.runButton.setText(running ? "停止运行" : "开始运行");
    this.runButton.setEnabled(true);
  }

  @Override
  public RunContext getCtx() {
    return this.ctx;
  }

  public ServerPanel getServerPanel() {
    return serverPanel;
  }

  @Override
  public String getSelectGroup() {
    return getServerPanel().getSelectGroup();
  }

  @Override
  public void initRobot() {
    this.robotPanel.reloadRobotConf(this);
  }

  @Override
  public void onStart() {}

  @Override
  public void onStop() {}

  @Override
  public int getStressTestType() {
    return this instanceof MessageWindow ? 1 : (this instanceof FunctionWindow ? 2 : 0);
  }

  /**
   * 获取压测消息列表
   *
   * @return
   */
  @Override
  public String getMessageId() {
    return null;
  }

  /**
   * 获取压测消息列表
   *
   * @return
   */
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
    return false;
  }

  @Override
  public int getMinRoundNumber() {
    return 1;
  }
  
  @Override
  public int getMaxRoundNumber() {
    return 1;
  }
  
  @Override
  public RunConf getRunConf() {
    return runConf;
  }

  @Override
  public void initFromArgv(String[] argv) {
    try {
      System.out.println("server start argv == " + argv);
      // 压测类型 1msg 2func
      int yaceType = Integer.parseInt(argv[0]);
      this.setVisible(yaceType == this.getStressTestType());

      // 格式 Server_2CL || Group_week
      if (!runConf.init) {
        runConf.init(argv[1]);
        runConf.robotConf.setRobotMaxNum(Integer.parseInt(argv[2]));
        runConf.robotConf.setRobotHoldNum(Integer.parseInt(argv[3]));
        runConf.robotConf.setSendDelayTime(1000);
        runConf.robotConf.setPrefixName(argv[4]);
        runConf.robotConf.single = false;

        this.robotPanel.getRobotMaxTextField().setText(runConf.robotConf.getRobotMaxNum() + "");
        this.robotPanel.getRobotHoldTextField().setText(runConf.robotConf.getRobotHoldNum() + "");
        this.robotPanel
            .getFunctionDelayTimeTextField()
            .setText(runConf.robotConf.getSendDelayTime() + "");
        this.robotPanel.getRobotNameTextField().setText(runConf.robotConf.getPrefixName() + "");

        // 开关 格式 1_1_1_1_1
        if (argv.length >= 6) {
          String[] split = StringUtils.split(argv[5], "_");
          this.consolePanel.setDebug("1".equals(split[0]));
          this.consolePanel.setScroll("1".equals(split[1]));
          this.consolePanel.setIncre("1".equals(split[2]));
          this.consolePanel.setLoginQue("1".equals(split[3]));
        } else {
          this.consolePanel.setDebug(false);
          this.consolePanel.setScroll(true);
          this.consolePanel.setIncre(false);
          this.consolePanel.setLoginQue(true);
        }
        this.updateChoosedServer();
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
      this.setVisible(
          e != null ? Integer.parseInt(e.getStringValue()) == this.getStressTestType() : false);

      // if (!runConf.init) {
      e = elRoot.element("server-group");
      String serverGroup = e != null ? e.getStringValue() : "Group@develop";
      runConf.init(serverGroup);
      runConf.robotConf.single = false;

      e = elRoot.element("max-robot");
      runConf.robotConf.setRobotMaxNum(e != null ? Integer.parseInt(e.getStringValue()) : 1000);
      this.robotPanel.getRobotMaxTextField().setText(runConf.robotConf.getRobotMaxNum() + "");

      e = elRoot.element("max-conn");
      runConf.robotConf.setRobotHoldNum(e != null ? Integer.parseInt(e.getStringValue()) : 50);
      this.robotPanel.getRobotHoldTextField().setText(runConf.robotConf.getRobotHoldNum() + "");

      e = elRoot.element("send-delay");
      runConf.robotConf.setSendDelayTime(e != null ? Integer.parseInt(e.getStringValue()) : 1000);
      this.robotPanel
          .getFunctionDelayTimeTextField()
          .setText(runConf.robotConf.getSendDelayTime() + "");

      e = elRoot.element("robot-name");
      runConf.robotConf.setPrefixName(e.getStringValue());
      this.robotPanel.getRobotNameTextField().setText(runConf.robotConf.getPrefixName() + "");

      e = elRoot.element("debug");
      this.consolePanel.setDebug(e != null ? Boolean.parseBoolean(e.getStringValue()) : false);

      e = elRoot.element("scroll");
      this.consolePanel.setScroll(e != null ? Boolean.parseBoolean(e.getStringValue()) : true);

      e = elRoot.element("incre");
      this.consolePanel.setIncre(e != null ? Boolean.parseBoolean(e.getStringValue()) : true);

      e = elRoot.element("login-queue");
      this.consolePanel.setLoginQue(e != null ? Boolean.parseBoolean(e.getStringValue()) : true);

      this.updateChoosedServer();
      // }
    } catch (DocumentException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public void updateChoosedServer() {
    runConf.init(null);
    this.serverPanel.updateChoosedServer(this);
  }

  protected JFrame frame;
  protected RobotPanel robotPanel;
  protected JButton runButton;
  protected JCheckBox topChose;
  private ConsolePanel consolePanel;
  private ServerPanel serverPanel;
  private RunContext ctx = new RunContext();
  private JRadioButton devRadioButton;
  private JRadioButton localRadioButton;

  protected static RunConf runConf = new RunConf();
}
