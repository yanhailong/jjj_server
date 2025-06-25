package com.jjg.game.UI.Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import com.jjg.game.UI.window.frame.FunctionWindow;
import com.jjg.game.core.event.EventScanner;

/** @function 功能确认事件 */
public class FunctionConfirmListener implements ActionListener {

  public FunctionConfirmListener(JDialog chooseFunctionDialog) {
    this.chooseFunctionDialog = chooseFunctionDialog;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    EventScanner.setFunctionMultpleEvents(FunctionWindow.toSelectedList());
    chooseFunctionDialog.dispose();
  }

  private JDialog chooseFunctionDialog;
}
