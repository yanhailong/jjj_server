package com.jjg.game.UI.window.frame;

import com.jjg.game.UI.entity.httpToolsEntities.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.xml.parsers.ParserConfigurationException;

import com.jjg.game.utils.HttpRequestUtils;
import org.xml.sax.SAXException;

/** 服务器http测试工具 */
public class HttpToolsWindow extends JDialog implements ActionListener, MouseListener {

  private static final long serialVersionUID = 1L;

  private String address;
  private JPanel contentPane;
  private JTextArea textArea;
  private JButton btnDoSwitch; // 开关功能的按钮
  private JButton btnDoCommand; // 有输入框的执行按钮
  private JList<String> lSelectedCommand;
  private JList<String> list_3;
  private JList<String> list_1;
  private JList<String> list;
  private DefaultListModel<String> onListModel1;
  private DefaultListModel<String> onListModel2;
  private Map<String, Integer> mouseMap;
  private Map<String, Object> itemList;
  private DefaultListModel<String> listForModeSelect;
  private Map<String, Map<String, String>> onListMap;

  public void setListForModeSelect(DefaultListModel<String> listForModeSelect) {
    this.listForModeSelect = listForModeSelect;
  }

  public DefaultListModel<String> getListForModeSelect() {
    return listForModeSelect;
  }

  public HttpToolsWindow(JFrame parent)
      throws SAXException, IOException, ParserConfigurationException {
    super(parent, true);
    /*
     * 获得xml中的数据
     */
    Map<String, Entity> data = ReadXml.getXMLDOM("config/http-command.xml");
    Entity entity = data.get("onList");
    List<Server> onList = entity.getSlist();
    String ip = "";
    String port = "";
    address = "http://" + ip + ":" + port + "/hqg/background_api/?";
    // =====================================================
    setResizable(false);
    setFont(new Font("宋体", Font.PLAIN, 12));
    setBounds(100, 100, 893, 530);
    setTitle("HTTP服务器功能开关");
    setAlwaysOnTop(true);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    setContentPane(contentPane);
    contentPane.setLayout(null);

    JPanel panel = new JPanel();
    panel.setBounds(0, 0, 877, 492);
    contentPane.add(panel);
    panel.setLayout(null);

    JLabel label = new JLabel("\u9009\u62E9\u529F\u80FD\u5217\u8868");
    label.setFont(new Font("宋体", Font.PLAIN, 12));
    label.setBounds(181, 28, 100, 15);
    panel.add(label);

    JLabel label_1 = new JLabel("\u6267\u884C\u529F\u80FD\u5217\u8868");
    label_1.setFont(new Font("宋体", Font.PLAIN, 12));
    label_1.setBounds(427, 28, 100, 15);
    panel.add(label_1);

    JLabel label_2 = new JLabel("\u5F00\u542F\u529F\u80FD\u5217\u8868");
    label_2.setFont(new Font("宋体", Font.PLAIN, 12));
    label_2.setBounds(181, 182, 100, 15);
    panel.add(label_2);

    JLabel label_3 = new JLabel("\u5173\u95ED\u529F\u80FD\u5217\u8868");
    label_3.setFont(new Font("宋体", Font.PLAIN, 12));
    label_3.setBounds(427, 182, 100, 15);
    panel.add(label_3);

    btnDoCommand = new JButton("执行");
    btnDoCommand.setFont(new Font("宋体", Font.PLAIN, 12));
    btnDoCommand.addActionListener(this);
    btnDoCommand.setBounds(678, 52, 93, 23);
    panel.add(btnDoCommand);

    btnDoSwitch = new JButton("执行");
    btnDoSwitch.setFont(new Font("宋体", Font.PLAIN, 12));
    btnDoSwitch.addActionListener(this);
    btnDoSwitch.setBounds(678, 205, 93, 23);
    panel.add(btnDoSwitch);

    JLabel lblNewLabel = new JLabel("控制台输出");
    lblNewLabel.setFont(new Font("宋体", Font.PLAIN, 12));
    lblNewLabel.setBounds(49, 315, 79, 15);
    panel.add(lblNewLabel);

    // 可输入参数的功能列表
    DefaultListModel<String> listModeSelect = new DefaultListModel<String>();
    listForModeSelect = new DefaultListModel<String>();

    itemList = new HashMap<String, Object>();
    Entity selectEntity = data.get("selectList");
    List<Server> selectList = selectEntity.getSlist();
    for (Server server : selectList) {
      String name = server.getName();
      String command = server.getCommand();
      listModeSelect.addElement(name);
      itemList.put(name, server.getParamList());
      String commandName = "command" + name;
      itemList.put(commandName, command);
    }

    // 设置滚动条
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setBounds(181, 53, 207, 97);
    panel.add(scrollPane);
    JScrollPane scrollPane4 = new JScrollPane();
    scrollPane4.setBounds(418, 53, 207, 97);
    panel.add(scrollPane4);

    lSelectedCommand = new JList<String>();
    lSelectedCommand.setFont(new Font("宋体", Font.PLAIN, 12));
    scrollPane4.setViewportView(lSelectedCommand);
    lSelectedCommand.setBorder(new LineBorder(new Color(0, 0, 0)));
    lSelectedCommand.setBounds(418, 53, 207, 97);
    lSelectedCommand.setModel(listForModeSelect);
    list = new JList<String>();
    list.setFont(new Font("宋体", Font.PLAIN, 12));
    scrollPane.setViewportView(list);
    list.setBorder(new LineBorder(new Color(0, 0, 0)));
    list.setModel(listModeSelect);
    mouseMap = new HashMap<String, Integer>();
    list.addMouseListener(this);

    // ===================可输入参数的功能列表结束==============================

    // onList的开关，放可以执行开关的操作
    onListModel1 = new DefaultListModel<String>();
    onListModel2 = new DefaultListModel<String>();
    onListMap = new HashMap<String, Map<String, String>>();
    for (Server server : onList) {
      String name = server.getName();
      Map<String, String> cMap = new HashMap<String, String>();
      cMap.put("command", server.getCommand());
      cMap.put("key", server.getKey());
      if ("false".equals(server.getValue()) || "0".equals(server.getValue())) {
        onListModel2.addElement(name);
      } else {
        onListModel1.addElement(name);
      }
      onListMap.put(name, cMap);
    }
    JScrollPane scrollPane_1 = new JScrollPane();
    scrollPane_1.setBounds(181, 207, 207, 97);
    panel.add(scrollPane_1);
    JScrollPane scrollPane_3 = new JScrollPane();
    scrollPane_3.setBounds(418, 207, 207, 98);
    panel.add(scrollPane_3);

    list_1 = new JList<String>();
    list_1.setFont(new Font("宋体", Font.PLAIN, 12));
    scrollPane_1.setViewportView(list_1);
    list_1.setBorder(new LineBorder(new Color(0, 0, 0)));
    list_1.setBounds(181, 207, 207, 98);
    list_1.setModel(onListModel1);

    list_1.addMouseListener(this);

    list_3 = new JList<String>();
    list_3.setFont(new Font("宋体", Font.PLAIN, 12));
    scrollPane_3.setViewportView(list_3);
    list_3.setBorder(new LineBorder(new Color(0, 0, 0)));
    list_3.setBounds(418, 207, 207, 98);
    list_3.setModel(onListModel2);
    list_3.addMouseListener(this);

    // =======================
    JScrollPane consoleScroll = new JScrollPane();
    consoleScroll.setBounds(51, 342, 720, 140);
    panel.add(consoleScroll);
    textArea = new JTextArea();
    textArea.setFont(new Font("宋体", Font.PLAIN, 12));
    textArea.setEditable(false);
    textArea.setBounds(51, 342, 600, 140);
    consoleScroll.setViewportView(textArea);
    setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    // 发送请求
    if (e.getSource() == btnDoSwitch) {
      new Thread(
              new Runnable() {
                @Override
                public void run() {
                  String re = null;
                  StringBuffer consoleStr = new StringBuffer();
                  Map<String, Map<String, String>> map = StaticEntity.getStaticEntity().getOnMap();
                  Set<String> set = map.keySet();
                  for (String str : set) {
                    Map<String, String> mm = map.get(str);
                    Map<String, String> params = new HashMap<>(16);
                    params.put("command", mm.get("command"));
                    params.put(mm.get("key"), mm.get("value"));
                    re = HttpRequestUtils.sendPost(address, params);
                    Server server = ReadXml.getServerTable().get(mm.get("command"));
                    consoleStr.append(server.getName()).append("\r\n").append(re).append("\r\n");
                  }
                  // 发完数据，删除数据
                  StaticEntity.getStaticEntity().getOnMap().clear();
                  textArea.append(consoleStr.toString());
                }
              })
          .start();
    } else if (e.getSource() == btnDoCommand) {
      new Thread(
              new Runnable() {

                @Override
                public void run() {

                  do {
                    if (address == null) {
                      JOptionPane.showMessageDialog(HttpToolsWindow.this, "请选择服务器");
                      break;
                    }

                    String result = null;
                    StringBuffer consoleStr = new StringBuffer();
                    Map<String, Map<String, String>> map = StaticEntity.getStaticEntity().getMap();

                    Set<String> set = map.keySet();
                    for (String str : set) {
                      StringBuffer sb = new StringBuffer();
                      Map<String, String> pMap = map.get(str);
                      Set<String> set2 = pMap.keySet();
                      for (String ss : set2) {
                        sb.append("&").append(ss).append("=").append(pMap.get(ss));
                      }
                      result = HttpRequestUtils.sendGet(address + "?" + sb);
                      Server server = ReadXml.getServerTable().get(pMap.get("command"));
                      consoleStr
                          .append(server.getName())
                          .append("\r\n")
                          .append(result)
                          .append("\r\n");
                    }
                    listForModeSelect.clear();
                    StaticEntity.getStaticEntity().getMap().clear();
                    textArea.append(consoleStr.toString());
                  } while (false);
                }
              })
          .start();
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getSource() == list_3) {
      String name = list_3.getSelectedValue();
      onListModel2.removeElement(name);
      list_3.setModel(onListModel2);
      onListModel1.addElement(name);
      list_1.setModel(onListModel1);
      Map<String, Map<String, String>> onMap = StaticEntity.getStaticEntity().getOnMap();
      if (onMap.containsKey(name)) {
        onMap.remove(name);
      } else {
        Map<String, String> pMap = onListMap.get(name);
        if (null != pMap) {
          pMap.put("value", "true");
          StaticEntity.getStaticEntity().setOnMap(name, pMap);
        }
      }
    }
    if (e.getSource() == list_1) {
      String name = list_1.getSelectedValue();

      onListModel1.removeElement(name);
      list_1.setModel(onListModel1);
      onListModel2.addElement(name);
      list_3.setModel(onListModel2);
      Map<String, Map<String, String>> onMap = StaticEntity.getStaticEntity().getOnMap();
      if (onMap.containsKey(name)) {
        onMap.remove(name);
      } else {
        Map<String, String> pMap = onListMap.get(name);
        if (null != pMap) {
          pMap.put("value", "false");
          StaticEntity.getStaticEntity().setOnMap(name, pMap);
        }
      }
    }
    if (e.getSource() == list) {
      String name = list.getSelectedValue();

      List<Parm> paramlist = (List<Parm>) itemList.get(name);
      String command = (String) itemList.get("command" + name);
      if (paramlist != null) {
        ParamWindow frame = new ParamWindow(paramlist, command, name, lSelectedCommand, this);
        frame.setVisible(true);
      } else {
        if (!listForModeSelect.contains(name)) {
          listForModeSelect.addElement(name);
          lSelectedCommand.setModel(listForModeSelect);
        }
      }
      mouseMap.put(name, null);
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {}

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}
}
