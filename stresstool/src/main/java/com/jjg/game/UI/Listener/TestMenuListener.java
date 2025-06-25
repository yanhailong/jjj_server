package com.jjg.game.UI.Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import com.jjg.game.UI.window.frame.FunctionWindow;
import com.jjg.game.UI.window.frame.MessageWindow;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.utils.FileEx;

/** @function 测试类型选项 */
public class TestMenuListener implements ActionListener {

  private JFrame curFrame;

  public TestMenuListener(JFrame globalFrame) {
    this.curFrame = globalFrame;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    curFrame.dispose();
    JMenuItem menuItem = (JMenuItem) e.getSource();
    switch (menuItem.getText()) {
      case "功能测试":
        FunctionWindow.getInstance().setVisible(true);
        MessageWindow.getInstance().setVisible(false);
        try {
          FileEx.writeAll("./config/windowIndex.txt", "1");
        } catch (IOException e1) {
          Log4jManager.getInstance().error(e1);
        }
        break;
      case "消息测试":
        FunctionWindow.getInstance().setVisible(false);
        MessageWindow.getInstance().setVisible(true);
        try {
          FileEx.writeAll("./config/windowIndex.txt", "2");
        } catch (IOException e1) {
          Log4jManager.getInstance().error(e1);
        }
        break;
    }
  }
}
