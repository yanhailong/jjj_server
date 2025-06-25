package com.jjg.game.UI.Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JFrame;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.jjg.game.UI.window.frame.HttpToolsWindow;

/** @function HTTP工具监听器 */
public class HttpToolsListener implements ActionListener {

  public HttpToolsListener(JFrame frame) {
    this.parentWindow = frame;
  }

  private JFrame parentWindow;

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      new HttpToolsWindow(parentWindow);
    } catch (SAXException | IOException | ParserConfigurationException e1) {
      e1.printStackTrace();
    }
  }
}
