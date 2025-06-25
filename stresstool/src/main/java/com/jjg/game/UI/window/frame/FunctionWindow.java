package com.jjg.game.UI.window.frame;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import org.apache.commons.lang.StringUtils;
import com.jjg.game.UI.Listener.RunFunctionListener;
import com.jjg.game.UI.Listener.SelecetFunctionListener;
import com.jjg.game.conf.RobotConstDefine;
import com.jjg.game.core.event.AbstractEvent;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.core.event.FunctionType;
import com.jjg.game.utils.MiscUtils;

/**
 * @function 功能测试窗口
 */
public class FunctionWindow extends MainWindow {

  public static DefaultListModel<String> waitingList = new DefaultListModel<>();
  public static DefaultListModel<String> selectedList = new DefaultListModel<>();

  public static FunctionWindow getInstance() {
    return Singleton.INSTANCE.getProcessor();
  }

  public JTextField getRobotMaxTextField() {
    return robotMaxTextField;
  }

  private FunctionWindow() {
    super();

    frame.setTitle("功能测试");
    frame.getContentPane().setFont(new Font("宋体", Font.PLAIN, 12));
    frame.setResizable(false);
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setBounds(
        (int) (dimension.getWidth() / 2 - (1220 >> 1)),
        (int) (dimension.getHeight() / 2 - (630 >> 1)),
        1220,
        630);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().setLayout(null);

    JMenuBar menuBar = new JMenuBar();
    menuBar.setBounds(0, 0, 1017, 21);
    frame.getContentPane().add(menuBar);

    JLabel titleLabel = new JLabel("功能测试");
    titleLabel.setFont(new Font("宋体", Font.BOLD, 20));
    titleLabel.setBounds(330, 29, 107, 26);
    frame.getContentPane().add(titleLabel);

    JLabel desLabel = new JLabel("");
    desLabel.setFont(new Font("宋体", Font.PLAIN, 18));
    desLabel.setBounds(298, 33, 186, 23);
    frame.getContentPane().add(desLabel);

    JButton chooseFunctionButton = new JButton("增删运行功能");
    chooseFunctionButton.setFont(new Font("宋体", Font.PLAIN, 12));
    chooseFunctionButton.setBounds(27, 444, 120, 23);
    chooseFunctionButton.addActionListener(new SelecetFunctionListener(this));
    frame.getContentPane().add(chooseFunctionButton);

    JScrollPane scrollPane1 = new JScrollPane();
    scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane1.setBounds(300, 159, 187, 382);
    frame.getContentPane().add(scrollPane1);

    functionChooseArea = new JTextArea();
    scrollPane1.setViewportView(functionChooseArea);
    functionChooseArea.setLineWrap(true);
    functionChooseArea.setEditable(false);
    functionChooseArea.setFont(new Font("宋体", Font.PLAIN, 12));

    JLabel label = new JLabel("已选功能列表:");
    label.setFont(new Font("宋体", Font.PLAIN, 12));
    label.setBounds(300, 65, 122, 15);
    frame.getContentPane().add(label);

    PAUSE_CHECK_BOX.setName("pauseCheckBox");
    PAUSE_CHECK_BOX.setBounds(300, 122, 93, 23);
    frame.getContentPane().add(PAUSE_CHECK_BOX);

    UNLIMIT_CHECK_BOX.setName("unlimitCheckBox");
    UNLIMIT_CHECK_BOX.setBounds(300, 92, 107, 23);
    frame.getContentPane().add(UNLIMIT_CHECK_BOX);

    runButton.addActionListener(new RunFunctionListener(this));

    frame.setVisible(true);
  }

  /** 显示已选择的功能 */
  public void showChooseMode() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < selectedList.size(); i++) {
      sb.append(selectedList.get(i)).append("\r\n");
    }
    functionChooseArea.setText(sb.toString());
    frame.repaint();
  }

  public static boolean isFunPause() {
    return PAUSE_CHECK_BOX.isSelected();
  }

  public static boolean isUnLimit() {
    return UNLIMIT_CHECK_BOX.isSelected();
  }

  private JTextField robotMaxTextField;
  private static JTextArea functionChooseArea;
  private static final JCheckBox PAUSE_CHECK_BOX = new JCheckBox("暂停开关");
  private static final JCheckBox UNLIMIT_CHECK_BOX = new JCheckBox("强制无限次");

  /** 用枚举来实现单例 */
  private enum Singleton {
    INSTANCE;

    final FunctionWindow processor;

    Singleton() {
      this.processor = new FunctionWindow();
    }

    FunctionWindow getProcessor() {
      return processor;
    }
  }

  /** 初始化功能模块选项 */
  public void initFunctionMode() {
    String sysGroup = System.getProperty("groups");
    int group = -1;
    int maxGroup = -1;
    if (sysGroup != null && !sysGroup.isEmpty()) {
      String[] split = StringUtils.split(sysGroup, "_");
      group = Integer.parseInt(split[0]);
      maxGroup = Integer.parseInt(split[1]);
    }

    int rc = FunctionType.FunCount.realCount;
    int stCount = group == 1 ? 0 : (rc / maxGroup) * (group - 1) + rc % maxGroup;
    int endCount = stCount + rc / maxGroup + (group == 1 ? rc % maxGroup : 0) - 1;

    Map<FunctionType, List<Class<? extends AbstractEvent<?>>>> multipleEventmap =
        EventScanner.getRequestMultipleEventClasses();
    Iterator<FunctionType> multipleIterator = multipleEventmap.keySet().iterator();

    int num = 0;
    while (multipleIterator.hasNext()) {
      FunctionType item = multipleIterator.next();
      String name = item.fName;

      if (name == null || name.isEmpty() || name.contains(RobotConstDefine.NO_CHOOSE_FUNCTION)) {
        continue;
      }

      // 可重复执行的模块显示设定的次数
      if (item.fNum > 0) {
        name += "_" + item.fNum + "次";
      } else {
        name += "_无限次";
      }

      if (group == -1 || maxGroup == -1) {
        if (MiscUtils.isIdeEnvironment()) {
          waitingList.addElement(name);
        } else {
          selectedList.addElement(name);
        }
      } else if (item.required) {
        selectedList.addElement(name);
      } else if (num <= endCount && num >= stCount) {
        selectedList.addElement(name);
        num++;
      } else {
        num++;
      }
    }

    EventScanner.setFunctionMultpleEvents(toSelectedList());
    showChooseMode();
  }

  public static List<String> toSelectedList() {
    List<String> fNames = new ArrayList<>();
    for (int i = 0; i < selectedList.size(); i++) {
      fNames.add(selectedList.get(i));
    }
    runConf.funcGroups = fNames;
    return fNames;
  }

  @Override
  public boolean isPause() {
    return isFunPause();
  }

  @Override
  public boolean isUnLimitTimes() {
    return isUnLimit();
  }

  @Override
  public void initFromArgv(String[] argv) {
    super.initFromArgv(argv);
    initFunctionMode();
  }

  @Override
  public void initFromXml(String path) {
    super.initFromXml(path);
    initFunctionMode();
  }
}
