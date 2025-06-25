package com.jjg.game.UI.window.frame;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import org.apache.commons.lang.StringUtils;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.jjg.game.UI.Listener.RunFunctionListener;
import com.jjg.game.UI.window.panel.MessagePanel;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.event.EventScanner;
import com.jjg.game.utils.ProtoBufUtils;
import com.jjg.game.utils.ProtoBufUtils.ProtoClientMessage;

/** @function 单接口测试 */
public class MessageWindow extends MainWindow {

  public static MessageWindow getInstance() {
    return Singleton.INSTANCE.getProcessor();
  }

  private MessageWindow() {
    super();

    MessageWindow my = this;

    frame.setTitle("消息测试");
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

    JLabel titlelLabel = new JLabel("消息测试");
    titlelLabel.setFont(new Font("宋体", Font.BOLD, 20));
    titlelLabel.setBounds(270, 33, 107, 26);
    frame.getContentPane().add(titlelLabel);

    // 显示demo消息信息
    String demo = "<html>单接口参数填写demo5122,5125,.....(逗号)<br>两个接口参数之间以|$|分割</html>";
    showDemoLable = new JLabel(demo);
    showDemoLable.setFont(new Font("宋体", Font.PLAIN, 13));
    showDemoLable.setBounds(270, 110, 270, 50);
    frame.add(showDemoLable);

    String msgId = "5122,5125";
    messageIdLabel = new JLabel("单接口id填写");
    messageIdLabel.setFont(new Font("宋体", Font.BOLD, 12));
    messageIdLabel.setBounds(270, 170, 161, 23);
    frame.add(messageIdLabel);

    robotHoldTextField = new JTextField(msgId);
    robotHoldTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
    robotHoldTextField.setColumns(10);
    robotHoldTextField.setBounds(270, 191, 161, 23);

    getMessagePanel().setLocation(270, 220);
    frame.getContentPane().add(getMessagePanel());

    msgIdButton = new JButton("确定id");
    msgIdButton.setFont(new Font("宋体", Font.PLAIN, 12));
    msgIdButton.setBounds(270, 260, 80, 23);
    frame.add(msgIdButton);

    msgIdRemoveButton = new JButton("移除id");
    msgIdRemoveButton.setFont(new Font("宋体", Font.PLAIN, 12));
    msgIdRemoveButton.setBounds(400, 260, 80, 23);
    frame.add(msgIdRemoveButton);

    // 显示的消息信息
    showMessagelabel = new JLabel("单接口参数填写");
    showMessagelabel.setFont(new Font("宋体", Font.BOLD, 13));
    // showMessagelabel.setSize(20, 100);
    showMessagelabel.setBounds(270, 290, 161, 23);
    frame.add(showMessagelabel);

    // 消息信息
    robotMessageTextField = new JTextField("1<>2|&|3<>true|$|10<>1<>true");
    robotMessageTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
    robotMessageTextField.setColumns(20);
    robotMessageTextField.setBounds(270, 311, 161, 23);

    paramButton = new JButton("确认");
    paramButton.setFont(new Font("宋体", Font.PLAIN, 12));
    paramButton.setBounds(270, 340, 80, 23);
    frame.add(paramButton);

    paramRemoveButton = new JButton("移除");
    paramRemoveButton.setFont(new Font("宋体", Font.PLAIN, 12));
    paramRemoveButton.setBounds(400, 340, 80, 23);
    frame.add(paramRemoveButton);

    allTest = new JCheckBox("全接口随机测试");
    allTest.setSelected(true);
    allTest.setBounds(290, 75, 161, 23);
    // JCheckBox 监听器
    allTest.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent arg0) {
            if (allTest.isSelected()) {
              messageIdLabel.setVisible(false);
              robotHoldTextField.setVisible(false);
              messageIdLabel.setVisible(false);
              msgIdButton.setVisible(false);
              msgIdRemoveButton.setVisible(false);
              robotMessageTextField.setVisible(false);
              showMessagelabel.setVisible(false);
              showDemoLable.setVisible(false);
              getMessagePanel().setVisible(false);
              paramRemoveButton.setVisible(false);
              paramButton.setVisible(false);
            } else {
              messageIdLabel.setVisible(true);
              robotHoldTextField.setVisible(true);
              messageIdLabel.setVisible(true);
              msgIdButton.setVisible(true);
              msgIdRemoveButton.setVisible(true);
              robotMessageTextField.setVisible(true);
              showMessagelabel.setVisible(true);
              showDemoLable.setVisible(true);
              getMessagePanel().setVisible(true);
              paramRemoveButton.setVisible(true);
              paramButton.setVisible(true);
            }
          }
        });
    frame.add(allTest);

    robotHoldTextField.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {
            checkMessageId(msgId, e, my);
          }

          @Override
          public void focusGained(FocusEvent e) {}
        });

    msgIdButton.addActionListener(
        new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            checkMessageInfo(null, my);
          }
        });

    msgIdRemoveButton.addActionListener(
        new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            robotHoldTextField.setText("");
          }
        });

    paramRemoveButton.addActionListener(
        new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            robotMessageTextField.setText("");
          }
        });

    frame.getContentPane().add(robotHoldTextField);
    frame.getContentPane().add(robotMessageTextField);

    messageIdLabel.setVisible(false);
    robotHoldTextField.setVisible(false);
    robotMessageTextField.setVisible(false);
    messageIdLabel.setVisible(false);
    msgIdButton.setVisible(false);
    paramRemoveButton.setVisible(false);
    paramButton.setVisible(false);
    showMessagelabel.setVisible(false);
    showDemoLable.setVisible(false);
    getMessagePanel().setVisible(false);
    msgIdRemoveButton.setVisible(false);

    runButton.addActionListener(new RunFunctionListener(this));

    robotPanel.getRobotCountLabel().setVisible(false);
    robotPanel.getRobotMaxTextField().setText("99999999");
    robotPanel.getRobotMaxTextField().setVisible(false);

    frame.setVisible(true);
  }

  /**
   * 取得当前设置的消息id
   *
   * @return
   */
  @Override
  public String getMessageId() {
    if (allTest.isSelected()) {
      return null;
    } else {
      return robotHoldTextField.getText();
    }
  }

  /** 取的当前设置的消息信息 * */
  @Override
  public String getMessageInfo() {
    if (allTest.isSelected()) {
      return null;
    } else {
      return robotMessageTextField.getText();
    }
  }

  public void setRobotHoldTextFields(String messageId) {
    if (allTest.isSelected()) return;
    String msg = robotHoldTextField.getText();
    if (msg != null && msg.length() > 0) {
      robotHoldTextField.setText(msg + "," + messageId);
    } else {
      robotHoldTextField.setText(messageId);
    }
  }

  JCheckBox allTest;
  JTextField robotHoldTextField;
  JTextField robotMessageTextField;
  JLabel messageIdLabel;
  JButton msgIdButton;
  JButton msgIdRemoveButton;

  JButton paramButton;
  JButton paramRemoveButton;

  JLabel showMessagelabel;
  // demo
  JLabel showDemoLable;
  MessagePanel messagePanel;

  /** 用枚举来实现单例 */
  private enum Singleton {
    INSTANCE;

    MessageWindow processor;

    Singleton() {
      this.processor = new MessageWindow();
    }

    MessageWindow getProcessor() {
      return processor;
    }
  }

  /** 在界面上显示message信息 * */
  void showTitleInfo(JLabel showMessagelabel) {
    List<ProtoClientMessage> exeMessage = new ArrayList<ProtoClientMessage>();
    String text = robotHoldTextField.getText();
    // 设置默认值
    String[] message_ids = text.split(",");
    StringBuffer showTitle = new StringBuffer();
    showTitle.append("<html>");
    List<ProtoClientMessage> clientMessages = EventScanner.getClientMessages();
    for (ProtoClientMessage protoClientMessage : clientMessages) {
      for (String messageId : message_ids) {
        if (protoClientMessage.getMsgId() == Integer.parseInt(messageId)) {
          exeMessage.add(protoClientMessage);
          if (!protoClientMessage.getBuilder().getAllFields().isEmpty()
              && protoClientMessage.getBuilder().getAllFields().size() > 0) {
            FieldDescriptor f =
                (FieldDescriptor)
                    protoClientMessage.getBuilder().getAllFields().keySet().toArray()[0];
            showTitle.append(f.getContainingType().getName() + ":<br/>");
          }
          try {
            ProtoBufUtils.createBuilder(protoClientMessage.getBuilder().getClass());
          } catch (Exception e) {
            e.printStackTrace();
          }
          for (Object o : protoClientMessage.getBuilder().getAllFields().keySet()) {
            FieldDescriptor fieldDescriptor = (FieldDescriptor) o;
            showTitle.append(
                fieldDescriptor.getNumber()
                    + ":"
                    + fieldDescriptor.getName()
                    + "("
                    + fieldDescriptor.getJavaType().toString().toLowerCase()
                    + ")<br/>");
          }
          showTitle.append("<br/>");
        }
      }
    }
    showTitle.append("</html>");
    showMessagelabel.setText(showTitle.toString());
  }

  /** 获取下来协议 * */
  public MessagePanel getMessagePanel() {
    if (messagePanel == null) {
      messagePanel = new MessagePanel();
    }
    return messagePanel;
  }

  /** 检测协议号 * */
  public void checkMessageId(String msgId, FocusEvent e, MessageWindow my) {
    JTextField textField = (JTextField) e.getSource();
    String text = textField.getText();
    if (text != null && text.length() > 2) {
      String[] confims = text.split(",");
      for (String confim : confims) {
        if (!confim.matches("^[0-9]{4}$")
            && !confim.matches("^[0-9]{5}$")
            && !confim.matches("^[0-9]{9}$")
            && !confim.matches("^[0-9]{3}$")) {
          JOptionPane.showMessageDialog(frame, "数量只能输入4或者5位数字");
        }
      }
      // 获取该消息ID的协议类型
      Map<Integer, ProtoClientMessage> messages = EventScanner.getClientMessageMap();
      List<ProtoClientMessage> protoClientMessages = new ArrayList<ProtoClientMessage>();
      for (String confim : confims) {
        ProtoClientMessage clientMessage = messages.get(Integer.valueOf(confim));
        if (clientMessage == null) {
          JOptionPane.showMessageDialog(frame, "找不到对应的消息id:" + confim);
          return;
        }
        protoClientMessages.add(clientMessage);
      }
      Log4jManager.getInstance().info(my, "当前消息 " + textField.getText() + " 对应的参数规范:");

      for (ProtoClientMessage clientMessage : protoClientMessages) {
        Map<FieldDescriptor, Object> map = clientMessage.getBuilder().getAllFields();
        Set<FieldDescriptor> key = map.keySet();
        // boolean create = false;
        Log4jManager.getInstance()
            .info(
                my,
                "Message:"
                    + "("
                    + clientMessage.getMsgId()
                    + ")"
                    + StringUtils.substringBefore(
                        StringUtils.substringAfter(
                            clientMessage.getBuilder().getClass().getName(),
                            "org.game.protobuf.c2s."),
                        "$Builder"));
        for (FieldDescriptor fieldDescriptor : key) {
          // if (!create) {
          // Log4jManager.getInstance().info(my,
          // fieldDescriptor.getContainingType().getName());
          // create = true;
          // }
          Log4jManager.getInstance()
              .info(
                  my,
                  "index:"
                      + (fieldDescriptor.getIndex() + 1)
                      + ",name:"
                      + fieldDescriptor.getName()
                      + ",type:"
                      + fieldDescriptor.getJavaType().toString().toLowerCase()
                      + ","
                      + (StringUtils.substringAfter(
                              fieldDescriptor.toProto().getLabel().toString(), "LABEL_"))
                          .toLowerCase());
        }
        Log4jManager.getInstance()
            .info(my, "------------------------------------------------------");
      }
    }
  }

  /** 检测协议号 * */
  public void checkMessageInfo(String msgId, MessageWindow my) {
    String text = robotHoldTextField.getText();
    if (text != null && text.length() > 2) {
      String[] confims = text.split(",");
      for (String confim : confims) {
        if (!confim.matches("^[0-9]{4}$")
            && !confim.matches("^[0-9]{5}$")
            && !confim.matches("^[0-9]{9}$")
            && !confim.matches("^[0-9]{3}$")) {
          JOptionPane.showMessageDialog(frame, "数量只能输入4或者5位数字");
        }
      }
      // 获取该消息ID的协议类型
      Map<Integer, ProtoClientMessage> messages = EventScanner.getClientMessageMap();
      List<ProtoClientMessage> protoClientMessages = new ArrayList<ProtoClientMessage>();
      for (String confim : confims) {
        ProtoClientMessage clientMessage = messages.get(Integer.valueOf(confim));
        if (clientMessage == null) {
          JOptionPane.showMessageDialog(frame, "找不到对应的消息id:" + confim);
          return;
        }
        protoClientMessages.add(clientMessage);
      }
      Log4jManager.getInstance().info(my, "当前消息 " + text + " 对应的参数规范:");

      for (ProtoClientMessage clientMessage : protoClientMessages) {
        Map<FieldDescriptor, Object> map = clientMessage.getBuilder().getAllFields();
        Set<FieldDescriptor> key = map.keySet();
        // boolean create = false;
        Log4jManager.getInstance()
            .info(
                my,
                "Message:"
                    + "("
                    + clientMessage.getMsgId()
                    + ")"
                    + StringUtils.substringBefore(
                        StringUtils.substringAfter(
                            clientMessage.getBuilder().getClass().getName(),
                            "org.game.protobuf.c2s."),
                        "$Builder"));
        for (FieldDescriptor fieldDescriptor : key) {
          // if (!create) {
          // Log4jManager.getInstance().info(my,
          // fieldDescriptor.getContainingType().getName());
          // create = true;
          // }
          Log4jManager.getInstance()
              .info(
                  my,
                  "index:"
                      + (fieldDescriptor.getIndex() + 1)
                      + ",name:"
                      + fieldDescriptor.getName()
                      + ",type:"
                      + fieldDescriptor.getJavaType().toString().toLowerCase()
                      + ","
                      + (StringUtils.substringAfter(
                              fieldDescriptor.toProto().getLabel().toString(), "LABEL_"))
                          .toLowerCase());
        }
        Log4jManager.getInstance()
            .info(my, "------------------------------------------------------");
      }
    }
  }
}
