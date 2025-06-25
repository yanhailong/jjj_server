package com.jjg.game.UI.window.panel;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jjg.game.conf.IConsole;
import com.jjg.game.core.robot.StressRobotManager;

public class ConsolePanel extends javax.swing.JPanel implements IConsole {

  public ConsolePanel() {
    setLayout(null);

    this.setBounds(0, 0, 590, 435);

    JLabel functionConsoleLabel = new JLabel("控制台消息:");
    functionConsoleLabel.setBounds(19, 12, 64, 15);
    functionConsoleLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
    add(functionConsoleLabel);

    printCheckBox = new JCheckBox("debug");
    printCheckBox.setBounds(88, 8, 84, 23);
    printCheckBox.setSelected(false);
    printCheckBox.setName("printCheckBox");
    printCheckBox.setEnabled(true);
    add(printCheckBox);

    scrollCheckBox = new JCheckBox("滚屏开关");
    scrollCheckBox.setBounds(168, 8, 84, 23);
    scrollCheckBox.setSelected(true);
    scrollCheckBox.setName("scrollCheckBox");
    add(scrollCheckBox);

    deltaCheckBox = new JCheckBox("增量开关");
    deltaCheckBox.setBounds(248, 8, 84, 23);
    deltaCheckBox.setName("deltaCheckBox");
    deltaCheckBox.setEnabled(true);
    add(deltaCheckBox);

    loginCheckBox = new JCheckBox("排队登陆");
    loginCheckBox.setBounds(328, 8, 84, 23);
    loginCheckBox.setName("deltaCheckBox");
    loginCheckBox.setSelected(true);
    loginCheckBox.setEnabled(true);
    add(loginCheckBox);

    JButton clearBtn = new JButton("清屏");

    clearBtn.addActionListener(
        e -> StressRobotManager.runBackPool(() -> functionConsoleArea.setText("")));

    clearBtn.setBounds(494, 6, 75, 29);
    clearBtn.setName("clearBtn");
    add(clearBtn);

    functionConsoleArea = new JTextArea();
    functionConsoleArea.setLineWrap(true);
    functionConsoleArea.setEditable(false);
    functionConsoleArea.setFont(new Font("宋体", Font.PLAIN, 12));
    functionConsoleArea
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void removeUpdate(DocumentEvent e) {
                StressRobotManager.runBackPool(
                    () -> {
                      if (scrollCheckBox.isSelected()) {
                        functionConsoleArea.setSelectionStart(
                            functionConsoleArea.getText().length());
                      }
                    });
              }

              @Override
              public void insertUpdate(DocumentEvent e) {
                StressRobotManager.runBackPool(
                    () -> {
                      if (scrollCheckBox.isSelected()) {
                        if (functionConsoleArea.getText().length() > 1000000) {
                          functionConsoleArea.setText("自动清屏");
                        }
                        functionConsoleArea.setSelectionStart(
                            functionConsoleArea.getText().length());
                      }
                    });
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                StressRobotManager.runBackPool(
                    () -> {
                      if (scrollCheckBox.isSelected()) {
                        functionConsoleArea.setSelectionStart(
                            functionConsoleArea.getText().length());
                      }
                    });
              }
            });

    JScrollPane scrollPane = new JScrollPane(functionConsoleArea);
    scrollPane.setBounds(6, 39, 578, 390);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    add(scrollPane);
  }

  /**
   * 控制面板添加打印信息
   *
   * @param actionMsg
   */
  @Override
  public void addConsoleAreaInfo(String actionMsg, boolean isDebug) {
    if (isDebug && !printCheckBox.isSelected()) {
      return;
    }
    if (functionConsoleArea == null) {
      return;
    }
    if (functionConsoleArea.getText().length() > 1000000) {
      functionConsoleArea.setText("");
    }
    functionConsoleArea.append(actionMsg + System.getProperty("line.separator"));
  }

  JTextArea functionConsoleArea;
  JCheckBox deltaCheckBox;
  JCheckBox printCheckBox;
  JCheckBox scrollCheckBox;
  JCheckBox loginCheckBox;
  JCheckBox chasmCheckBox;

  private static final long serialVersionUID = 1L;

  @Override
  public boolean isDebug() {
    return this.printCheckBox.isSelected();
  }

  @Override
  public boolean isScroll() {
    return this.scrollCheckBox.isSelected();
  }

  @Override
  public boolean isIncre() {
    return this.deltaCheckBox.isSelected();
  }

  @Override
  public boolean isLoginQue() {
    return this.loginCheckBox.isSelected();
  }

  @Override
  public void setDebug(boolean _debug) {
    this.printCheckBox.setSelected(_debug);
  }

  @Override
  public void setScroll(boolean _scroll) {
    this.scrollCheckBox.setSelected(_scroll);
  }

  @Override
  public void setIncre(boolean _incre) {
    this.deltaCheckBox.setSelected(_incre);
  }

  @Override
  public void setLoginQue(boolean _LoginQue) {
    this.loginCheckBox.setSelected(_LoginQue);
  }
}
