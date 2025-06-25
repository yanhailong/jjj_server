package com.jjg.game.UI.window.panel;

import com.jjg.game.UI.window.frame.MainWindow;
import com.jjg.game.conf.IConsole;
import com.jjg.game.conf.IMainWindow;
import com.jjg.game.conf.RobotConf;
import com.jjg.game.conf.RunConf;
import com.jjg.game.utils.ExceptionEx;
import com.jjg.game.utils.FileEx;
import com.jjg.game.utils.LoggerUtils;
import com.jjg.game.utils.StrEx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RobotPanel extends javax.swing.JPanel {

  public RobotPanel(MainWindow mainWindow) {
    setLayout(null);
    this.setBounds(0, 0, 180, 300);

    parent = this.getParent();

    robotCountLabel = new JLabel("最大机器人数量:");
    robotCountLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    robotCountLabel.setBounds(6, 6, 107, 15);
    add(robotCountLabel);

    robotMaxTextField = new JTextField("10");
    robotMaxTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
    robotMaxTextField.setColumns(10);
    robotMaxTextField.setBounds(6, 27, 66, 21);
    robotMaxTextField.setText(String.valueOf(1));
    robotMaxTextField.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {}

          @Override
          public void focusLost(FocusEvent e) {
            JTextField textField = (JTextField) e.getSource();
            if (!textField
                .getText()
                .matches("^[1-9][0-9]{0,4}$")) {
              JOptionPane.showMessageDialog(parent, "数量只能输入1-99999");
              textField.setText("10");
            } else {
              saveRobotInfo();
            }
          }
        });
    add(robotMaxTextField);

    JLabel holdClientlabel = new JLabel("链接保持数：");
    holdClientlabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    holdClientlabel.setBounds(6, 60, 107, 15);
    add(holdClientlabel);

    robotHoldTextField = new JTextField("1");
    robotHoldTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
    robotHoldTextField.setColumns(10);
    robotHoldTextField.setBounds(6, 88, 66, 21);
    robotHoldTextField.addFocusListener(
        new FocusListener() {

          @Override
          public void focusLost(FocusEvent e) {
            JTextField textField = (JTextField) e.getSource();
            if (!textField
                .getText()
                .matches("^[1-9][0-9]{0,4}$")) {
              JOptionPane.showMessageDialog(parent, "数量只能输入1-99999");
              textField.setText("1");
            } else {
              saveRobotInfo();
            }
          }

          @Override
          public void focusGained(FocusEvent e) {}
        });
    add(robotHoldTextField);

    JButton reloadbutton = new JButton("更新机器人数量");
    reloadbutton.setName("reloadbutton");
    reloadbutton.setFont(new Font("Dialog", Font.PLAIN, 12));
    reloadbutton.setBounds(6, 123, 120, 23);
    reloadbutton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (mainWindow.getServerPanel().getIsRandomCheckBox()
                && "1".equals(robotHoldTextField.getText())
                && "-1".equals(robotMaxTextField.getText())) {
              robotHoldTextField.setText("5000");
              robotMaxTextField.setText("100000");
              IConsole console = mainWindow.getConsole();
              console.setIncre(true);
              console.setLoginQue(false);
            }
            reloadRobotConf(mainWindow);
          }
        });
    add(reloadbutton);

    JLabel functionDelayTimeTextLabel = new JLabel("发送间隔(毫秒)");
    functionDelayTimeTextLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
    functionDelayTimeTextLabel.setBounds(16, 161, 107, 15);
    add(functionDelayTimeTextLabel);

    functionDelayTimeTextField = new JTextField("2000");
    functionDelayTimeTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
    functionDelayTimeTextField.setColumns(10);
    functionDelayTimeTextField.setBounds(6, 188, 66, 21);
    functionDelayTimeTextField.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {
            JTextField textField = (JTextField) e.getSource();
            if (!textField.getText().matches("^(([1-9]{1}\\d*)|([0]{1}))(\\.(\\d){0,2})?$")) {
              JOptionPane.showMessageDialog(parent, "只能是数字且不能以0开头");
              textField.setText("2000");
            } else {
              saveRobotInfo();
            }
          }

          @Override
          public void focusGained(FocusEvent e) {}
        });
    add(functionDelayTimeTextField);

    JLabel robotNameLabel = new JLabel("机器人名称(accountId):");
    robotNameLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
    robotNameLabel.setBounds(6, 256, 250, 15);
    add(robotNameLabel);

    String name;
    try {
      name = InetAddress.getLocalHost().getHostName();
      int index = name.indexOf(".");
      if (index != -1) {
        name = StrEx.removeRight(name, name.length() - index);
      }
    } catch (UnknownHostException e) {
      name = "robot";
    }
    robotNameTextField = new JTextField(name);
    robotNameTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
    robotNameTextField.setColumns(10);
    robotNameTextField.setBounds(6, 273, 151, 21);
    robotNameTextField.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {
            JTextField textField = (JTextField) e.getSource();
            if (StrEx.isEmpty(textField.getText())) {
              JOptionPane.showMessageDialog(parent, "请填写机器人名称");
              textField.setText("test");
            } else {
              saveRobotInfo();
            }
          }

          @Override
          public void focusGained(FocusEvent e) {}
        });
    add(robotNameTextField);

    isSingleCheckBox = new JCheckBox("固定用户名|且不增加后缀");
    isSingleCheckBox.setBounds(6, 221, 250, 23);
    isSingleCheckBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (isSingleCheckBox.isSelected()) {
              robotHoldTextField.setText("1");
              robotHoldTextField.setEnabled(false);
              robotMaxTextField.setEnabled(false);
            } else {
              robotHoldTextField.setEnabled(true);
              robotMaxTextField.setEnabled(true);
            }
            saveRobotInfo();
          }
        });
    add(isSingleCheckBox);

    // 加载机器人的配置
    RunConf runConf = mainWindow.getRunConf();
    if (runConf.loadRobotInfo()) {
      RobotConf robotConf = runConf.getRobotConf();
      robotMaxTextField.setText(Integer.toString(robotConf.getRobotMaxNum()));
      robotHoldTextField.setText(Integer.toString(robotConf.getRobotHoldNum()));
      functionDelayTimeTextField.setText(Long.toString(robotConf.getSendDelayTime()));
      robotNameTextField.setText(robotConf.getPrefixName());
      isSingleCheckBox.setSelected(robotConf.isSingle());
      if (isSingleCheckBox.isSelected()) {
        robotHoldTextField.setEnabled(false);
        robotMaxTextField.setEnabled(false);
      }
    }
  }

  private void saveRobotInfo() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(robotMaxTextField.getText());
    stringBuilder.append(System.lineSeparator());
    stringBuilder.append(robotHoldTextField.getText());
    stringBuilder.append(System.lineSeparator());
    stringBuilder.append(functionDelayTimeTextField.getText());
    stringBuilder.append(System.lineSeparator());
    stringBuilder.append(robotNameTextField.getText());
    stringBuilder.append(System.lineSeparator());
    stringBuilder.append(isSingleCheckBox.isSelected() ? 1 : 0);

    try {
      FileEx.writeAll("./config/robotInfo.txt", stringBuilder.toString());
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      System.exit(1);
    }
  }

  public boolean getIsSingle() {
    return isSingleCheckBox.isSelected();
  }

  public JTextField getRobotMaxTextField() {
    return robotMaxTextField;
  }

  public JLabel getRobotCountLabel() {
    return robotCountLabel;
  }

  public JTextField getRobotHoldTextField() {
    return robotHoldTextField;
  }

  public JTextField getFunctionDelayTimeTextField() {
    return functionDelayTimeTextField;
  }

  public JTextField getRobotNameTextField() {
    return robotNameTextField;
  }

  /**
   * 初始化运行机器人配置
   *
   * @param window
   */
  public void reloadRobotConf(IMainWindow window) {
    int robotCount = Integer.valueOf(robotMaxTextField.getText());
    long msgDelayTime = Long.valueOf(functionDelayTimeTextField.getText());
    String robotName = robotNameTextField.getText();
    int holdNum = Integer.valueOf(robotHoldTextField.getText());
    window.getRunConf().robotConf.setRobotMaxNum(robotCount);
    window.getRunConf().robotConf.setSendDelayTime(msgDelayTime);
    window.getRunConf().robotConf.setPrefixName(robotName);
    window.getRunConf().robotConf.setRobotHoldNum(holdNum);
    window.getRunConf().robotConf.setSingle(this.getIsSingle());
  }

  private static final long serialVersionUID = 1L;
  private JTextField robotMaxTextField;
  private JTextField robotHoldTextField;
  private JTextField functionDelayTimeTextField;
  private JTextField robotNameTextField;
  private Container parent;
  private JLabel robotCountLabel;
  private JCheckBox isSingleCheckBox;
}
