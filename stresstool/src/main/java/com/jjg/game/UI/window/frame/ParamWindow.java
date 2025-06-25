package com.jjg.game.UI.window.frame;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.jjg.game.utils.LoggerUtils;
import org.apache.commons.lang.StringUtils;
import com.jjg.game.UI.entity.httpToolsEntities.Parm;
import com.jjg.game.UI.entity.httpToolsEntities.StaticEntity;

/** 输入参数的弹出框 */
public class ParamWindow extends JDialog implements ActionListener {

  private static final long serialVersionUID = 1L;
  private JPanel contentPane;
  private JTextField text = null;
  private JPanel panel;
  private Map<String, String> toolMap;
  private String name;
  private JList<String> lSelectedCommand;
  private String command;
  private Map<String, String> map;
  private JTextField customParamTF = new JTextField(); // 自定义参数

  public static Set<String> commandNeedCustomParam = new HashSet<String>();

  static {
    String[] arr = {"mail", "calljavascript"};
    Collections.addAll(commandNeedCustomParam, arr);
  }

  /** Create the frame. */
  public ParamWindow(
      List<Parm> paramList,
      String command,
      String name,
      JList<String> lSelectedCommand,
      JDialog parent) {
    super(parent, true);

    this.command = command;
    this.lSelectedCommand = lSelectedCommand;
    this.name = name;
    setResizable(false);
    setBounds(100, 100, 450, 386);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

    setContentPane(contentPane);
    contentPane.setLayout(null);

    panel = new JPanel();
    panel.setBounds(0, 0, 434, 348);
    contentPane.add(panel, BorderLayout.WEST);
    panel.setLayout(null);

    JButton btnNewButton = new JButton("取消");
    btnNewButton.setFont(new Font("宋体", Font.PLAIN, 12));
    btnNewButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            // dispose();
            setVisible(false);
          }
        });
    btnNewButton.setBounds(79, 268, 93, 23);
    panel.add(btnNewButton);
    map = new HashMap<String, String>();
    JPanel jPanel = null;
    Integer locationY = 1;
    if (paramList != null) {
      for (Parm parm : paramList) {
        // Set<String> set = parm.getMap().keySet();
        jPanel = new JPanel();
        jPanel.setLayout(null);
        jPanel.setBounds(50, locationY * 35, 400, 30);

        JLabel label = new JLabel(parm.getMap().get("name"));
        label.setFont(new Font("宋体", Font.PLAIN, 12));
        text = new JTextField();
        if (!(parm.getMap().get(parm.getMap().get("value")) == null
            || parm.getMap().get(parm.getMap().get("value")) == "")) {
          text.setText(parm.getMap().get(parm.getMap().get("value")).toString().trim());
        }
        text.setBounds(130, 0, 200, 30);
        label.setBounds(0, 0, 100, 15);
        map.put(parm.getMap().get("name"), parm.getMap().get("key"));

        jPanel.add(text);
        jPanel.add(label);

        panel.add(jPanel);
        locationY++;
      }
    }

    if (commandNeedCustomParam.contains(command)) {
      JLabel lAddCustomParam = new JLabel("自定义参数");
      lAddCustomParam.setBounds(50, locationY * 35, 100, 15);
      lAddCustomParam.setFont(new Font("宋体", Font.PLAIN, 12));
      panel.add(lAddCustomParam);
      JLabel lFormat = new JLabel("格式：key1,value1;key2,value2...");
      lFormat.setFont(new Font("宋体", Font.PLAIN, 12));
      lFormat.setBounds(180, locationY * 35 + 35, 200, 15);
      panel.add(lFormat);

      customParamTF.setBounds(180, locationY * 35, 200, 30);
      panel.add(customParamTF);
    }

    JButton btnNewButton_1 = new JButton("确定");
    btnNewButton_1.setFont(new Font("宋体", Font.PLAIN, 12));
    toolMap = new HashMap<String, String>();
    btnNewButton_1.addActionListener(this);
    btnNewButton_1.setBounds(237, 268, 93, 23);
    panel.add(btnNewButton_1);
  }

  // 点击按钮将左边List中数据添加到右边List中，并将数据存到StaticEntity中。
  @Override
  public void actionPerformed(ActionEvent e) {
    Component[] conmponentlist = panel.getComponents();
    for (int i = 0; i < conmponentlist.length; i++) {
      Component component = conmponentlist[i];

      if (component instanceof JPanel) {
        JPanel pp = (JPanel) component;
        pp.setFont(new Font("宋体", Font.PLAIN, 12));
        Component[] list = pp.getComponents();
        String value = null;
        String gname = null;
        for (int j = 0; j < list.length; j++) {
          if (list[j] instanceof JTextField) {
            JTextField tt = (JTextField) list[j];
            LoggerUtils.LOGGER.info(tt.getText() + "...");
            value = tt.getText();
          }
          if (list[j] instanceof JLabel) {
            JLabel ll = (JLabel) list[j];
            System.out.println(ll.getText());
            gname = map.get(ll.getText());
          }
        }
        toolMap.put(gname, value);
      }
    }
    toolMap.put("command", command);
    if (commandNeedCustomParam.contains(command)) toolMap.putAll(getCustomParam());
    boolean success = StaticEntity.setMap(toolMap, name);
    if (success) {
      setVisible(false);
      DefaultListModel<String> modelList = (DefaultListModel<String>) lSelectedCommand.getModel();
      modelList.addElement(name);
      lSelectedCommand.setModel(modelList);
    } else {
      JOptionPane.showMessageDialog(null, "已经添加了此指令", "警告", JOptionPane.WARNING_MESSAGE);
    }
  }

  private Map<? extends String, ? extends String> getCustomParam() {
    Map<String, String> paramMap = new HashMap<String, String>();
    String paramsString = customParamTF.getText();
    if (!StringUtils.isEmpty(paramsString)) {
      for (String paramString : paramsString.split(";")) {
        String[] param = paramString.split(",");
        paramMap.put(param[0], param[1]);
      }
    }
    return paramMap;
  }
}
