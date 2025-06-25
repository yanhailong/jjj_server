package com.jjg.game.UI.Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.jjg.game.UI.window.frame.ChooseFunctionWindow;
import com.jjg.game.UI.window.frame.FunctionWindow;

/** @function 选择功能监听器 */
public class SelecetFunctionListener implements ActionListener {

  public SelecetFunctionListener(FunctionWindow window) {
    this.window = window;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    new ChooseFunctionWindow(window);
  }

  private FunctionWindow window;
}
