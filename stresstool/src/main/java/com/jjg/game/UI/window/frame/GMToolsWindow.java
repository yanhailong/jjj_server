package com.jjg.game.UI.window.frame;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

/** 为自定义账号开启GM权限 */
public class GMToolsWindow extends JDialog {

  private static final long serialVersionUID = 1L;
  private JTextField usernameTextField;
  private JTextField commandArgsTextField;

  public GMToolsWindow(JFrame parent) {
    super(parent, true);
    getContentPane().setLayout(null);

    JLabel usernameLabel = new JLabel("账号：");
    usernameLabel.setFont(new Font("宋体", Font.PLAIN, 12));
    usernameLabel.setBounds(10, 10, 61, 15);
    getContentPane().add(usernameLabel);

    usernameTextField = new JTextField();
    usernameTextField.setFont(new Font("宋体", Font.PLAIN, 12));
    usernameTextField.setBounds(92, 7, 131, 21);
    getContentPane().add(usernameTextField);
    usernameTextField.setColumns(10);

    JButton loginButton = new JButton("登录");
    loginButton.setFont(new Font("宋体", Font.PLAIN, 12));
    loginButton.setBounds(10, 53, 93, 23);
    getContentPane().add(loginButton);

    JComboBox commandsComboBox = new JComboBox();
    commandsComboBox.setFont(new Font("宋体", Font.PLAIN, 12));
    commandsComboBox.setEditable(true);
    commandsComboBox.setBounds(92, 107, 131, 21);
    getContentPane().add(commandsComboBox);

    JLabel commandLabel = new JLabel("GM命令:");
    commandLabel.setFont(new Font("宋体", Font.PLAIN, 12));
    commandLabel.setBounds(10, 110, 61, 15);
    getContentPane().add(commandLabel);

    JButton doCommandButton = new JButton("发送命令");
    doCommandButton.setFont(new Font("宋体", Font.PLAIN, 12));
    doCommandButton.setBounds(10, 201, 93, 23);
    doCommandButton.setEnabled(false);
    getContentPane().add(doCommandButton);

    JTextArea consoleTextArea = new JTextArea();
    consoleTextArea.setBorder(new LineBorder(new Color(0, 0, 0)));
    consoleTextArea.setFont(new Font("宋体", Font.PLAIN, 13));
    consoleTextArea.setEditable(false);
    consoleTextArea.setBounds(399, 6, 268, 340);
    getContentPane().add(consoleTextArea);

    JLabel consoleLabel = new JLabel("控制台消息:");
    consoleLabel.setFont(new Font("宋体", Font.PLAIN, 12));
    consoleLabel.setBounds(266, 10, 87, 15);
    getContentPane().add(consoleLabel);

    JLabel commandArgsLabel = new JLabel("GM命令参数:");
    commandArgsLabel.setFont(new Font("宋体", Font.PLAIN, 12));
    commandArgsLabel.setBounds(10, 149, 73, 15);
    getContentPane().add(commandArgsLabel);

    commandArgsTextField = new JTextField();
    commandArgsTextField.setFont(new Font("宋体", Font.PLAIN, 12));
    commandArgsTextField.setColumns(10);
    commandArgsTextField.setBounds(92, 146, 131, 21);
    getContentPane().add(commandArgsTextField);

    JButton commandsDetailButton = new JButton("参数详情");
    commandsDetailButton.setFont(new Font("宋体", Font.PLAIN, 12));
    commandsDetailButton.setBounds(260, 145, 93, 23);
    getContentPane().add(commandsDetailButton);
    initialize();
  }

  private void initialize() {
    this.setBounds(100, 100, 683, 384);
    this.setResizable(false);
    this.setTitle("GM工具");
    this.setVisible(true);
  }
}
