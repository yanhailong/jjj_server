package com.jjg.game.UI.Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultListModel;
import com.jjg.game.UI.window.frame.FunctionWindow;

/** @function 全选功能监听器 */
public class AllFunctionChooseListener implements ActionListener {

  private DefaultListModel<String> sourceListMode;
  private DefaultListModel<String> targetListMode;
  private FunctionWindow window;

  public AllFunctionChooseListener(
      FunctionWindow window,
      DefaultListModel<String> sourceListMode,
      DefaultListModel<String> targetListMode) {
    this.sourceListMode = sourceListMode;
    this.targetListMode = targetListMode;
    this.window = window;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (sourceListMode.size() > 0) {
      for (int i = 0; i < sourceListMode.size(); i++) {
        targetListMode.addElement(sourceListMode.get(i));
      }
      sourceListMode.clear();
    }
    window.showChooseMode();
  }
}
