package com.jjg.game.UI.window.panel;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.jjg.game.conf.IMainWindow;
import com.jjg.game.conf.ServerConf;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.utils.FileEx;

public class ServerPanel extends javax.swing.JPanel {

  public ServerPanel(IMainWindow mainWindow) {
    setLayout(null);

    this.setBounds(0, 0, 600, 80);

    comboBox = new JComboBox<ServerConf>();
    comboBox.setBounds(6, 6, 200, 30);
    comboBox.setFont(new Font("Dialog", Font.BOLD, 18));
    add(comboBox);

    JLabel serverInfoLabel = new JLabel("服务器信息: " + mainWindow.getRunConf().choosedServer);
    serverInfoLabel.setBounds(16, 46, 372, 25);
    serverInfoLabel.setFont(new Font("宋体", Font.PLAIN, 18));
    add(serverInfoLabel);

    isRandomCheckBox = new JCheckBox("随机分发,分组名:");
    isRandomCheckBox.setBounds(250, 10, 150, 23);
    isRandomCheckBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (isRandomCheckBox.isSelected()) {
              mainWindow.getRunConf().choosedServer = (ServerConf) groupBox.getSelectedItem();
              serverInfoLabel.setText("服务器信息: " + groupBox.getSelectedItem());
              comboBox.setEnabled(false);
              groupBox.setEnabled(true);
            } else {
              mainWindow.getRunConf().choosedServer = (ServerConf) comboBox.getSelectedItem();
              serverInfoLabel.setText("服务器信息: " + comboBox.getSelectedItem());
              comboBox.setEnabled(true);
              groupBox.setEnabled(false);
            }
          }
        });
    add(isRandomCheckBox);

    groupBox = new JComboBox<ServerConf>();
    groupBox.setBounds(400, 6, 100, 30);
    groupBox.setFont(new Font("Dialog", Font.BOLD, 14));
    groupBox.setEnabled(false);
    add(groupBox);

    mainWindow
        .getRunConf()
        .SERVERCONFS
        .forEach(
            server -> {
              if (server.getType().equals("Server")) {
                comboBox.addItem(server);
              } else if (server.getType().equals("Group")) {
                groupBox.addItem(server);
              }
            });
    groupBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            mainWindow.getRunConf().choosedServer = groupBox.getItemAt(groupBox.getSelectedIndex());
            serverInfoLabel.setText("服务器信息: " + mainWindow.getRunConf().choosedServer);

            if (mainWindow.getRunConf().choosedServer != null) {
              try {
                ServerConf conf = ((ServerConf) groupBox.getSelectedItem());
                String server = conf.getType() + "_" + conf.getName();
                FileEx.writeAll("./config/serverIndex.txt", server);
              } catch (IOException ex) {
                Log4jManager.getInstance().error(ex);
              }
            }
          }
        });

    comboBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            mainWindow.getRunConf().choosedServer = comboBox.getItemAt(comboBox.getSelectedIndex());
            serverInfoLabel.setText("服务器信息: " + mainWindow.getRunConf().choosedServer);

            if (mainWindow.getRunConf().choosedServer != null) {
              try {
                ServerConf conf = ((ServerConf) comboBox.getSelectedItem());
                String server = conf.getType() + "_" + conf.getName();
                FileEx.writeAll("./config/serverIndex.txt", server);
              } catch (IOException ex) {
                Log4jManager.getInstance().error(ex);
              }
            }
          }
        });
    if (mainWindow.getRunConf().choosedServer != null) {
      comboBox.setSelectedItem(mainWindow.getRunConf().choosedServer);
    }
  }

  public void updateChoosedServer(IMainWindow window) {
    ServerConf sc = window.getRunConf().choosedServer;
    if (sc != null) {
      if (sc.getType().equals("Server")) {
        comboBox.setSelectedItem(sc);
        isRandomCheckBox.setSelected(false);
        comboBox.setEnabled(true);
        groupBox.setEnabled(false);
      } else {
        isRandomCheckBox.setSelected(true);
        groupBox.setSelectedItem(sc);
        comboBox.setEnabled(false);
        groupBox.setEnabled(true);
      }
    }
  }

  public boolean getIsRandomCheckBox() {
    return isRandomCheckBox.isSelected();
  }

  public String getSelectGroup() {
    return String.valueOf(groupBox.getSelectedItem());
  }

  private JComboBox<ServerConf> groupBox;
  private JComboBox<ServerConf> comboBox;
  private JCheckBox isRandomCheckBox;
  private static final long serialVersionUID = 1L;
}
