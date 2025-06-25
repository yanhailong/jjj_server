package com.jjg.game.UI.window.panel;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import org.apache.commons.lang.StringUtils;
import com.jjg.game.UI.window.frame.MessageWindow;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.utils.ProtoBufUtils.ProtoClientMessage;

/**
 * *
 *
 * <p>单接口消息号的下拉框
 *
 * @author
 */
public class MessagePanel extends javax.swing.JPanel {

  private static final long serialVersionUID = 1L;

  public MessagePanel() {
    setLayout(null);

    this.setBounds(0, 0, 345, 30);

    JComboBox<String> comboBox = new JComboBox<String>();
    comboBox.setBounds(0, 0, 328, 30);
    comboBox.setFont(new Font("Dialog", Font.BOLD, 12));
    List<ProtoClientMessage> clientMessages = resort();
    for (ProtoClientMessage protoClientMessage : clientMessages) {
      comboBox.addItem(
          "("
              + protoClientMessage.getMsgId()
              + ")"
              + StringUtils.substringBefore(
                  StringUtils.substringAfter(
                      protoClientMessage.getBuilder().getClass().getName(),
                      "org.game.protobuf.c2s."),
                  "$Builder"));
    }
    add(comboBox);
    comboBox.addItemListener(
        new ItemListener() {

          @Override
          public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
              List<ProtoClientMessage> clientMessages = resort();
              MessageWindow.getInstance()
                  .setRobotHoldTextFields(
                      clientMessages.get(comboBox.getSelectedIndex()).getMsgId() + "");
            }
          }
        });
  }

  /** 按照协议号的顺序进行排序 * */
  public List<ProtoClientMessage> resort() {
    List<ProtoClientMessage> resort = new ArrayList<ProtoClientMessage>();
    List<String> resortNames = new ArrayList<String>();
    List<ProtoClientMessage> protoClientMessage = EventScanner.getClientMessages();
    for (ProtoClientMessage message : protoClientMessage) {
      String name = message.getBuilder().getClass().getTypeName().split("\\$")[0];
      if (!resortNames.contains(name)) {
        resortNames.add(name);
      }
    }
    for (String name : resortNames) {
      for (ProtoClientMessage message : protoClientMessage) {
        String addName = message.getBuilder().getClass().getTypeName().split("\\$")[0];
        if (name.equals(addName)) {
          resort.add(message);
        }
      }
    }
    return resort;
  }
}
