package com.jjg.game.UI.entity.httpToolsEntities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** 读取XML和解析XML */
public class ReadXml {

  private static Map<String, Server> serverTable = new ConcurrentHashMap<>();

  public static Map<String, Entity> getXMLDOM(String path)
      throws SAXException, IOException, ParserConfigurationException {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

    Document doc = dBuilder.parse(path);

    NodeList serverlist = doc.getElementsByTagName("serverlist");
    NodeList serverType = doc.getElementsByTagName("serverType");
    NodeList selectList = doc.getElementsByTagName("selectList");
    NodeList onList = doc.getElementsByTagName("onList");

    Map<String, Entity> serverMap = new HashMap<String, Entity>();

    serverMap.put("serverlist", getMap(serverlist));
    serverMap.put("serverType", getMap(serverType));
    serverMap.put("selectList", getMap(selectList));
    serverMap.put("onList", getMap(onList));

    return serverMap;
  }

  public static Entity getMap(NodeList list) {
    // Map<String,Object> pMap = new HashMap<String,Object>();
    Entity entityList = new Entity();
    List<Server> serverList = new ArrayList<Server>();
    int serverNum = 0;
    for (int i = 0; i < list.getLength(); i++) {
      Node server = list.item(i);
      for (Node node = server.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Server server2 = new Server();
          Element elem = (Element) node;
          // String name = node.getNodeName();
          String name = elem.getAttribute("name");
          String value = elem.getAttribute("value");
          String command = elem.getAttribute("command");
          String ip = elem.getAttribute("address");
          // System.out.println(name + value);
          String key = elem.getAttribute("key");
          NodeList nodelist = elem.getElementsByTagName("parm");
          // System.out.println(nodelist.getLength());
          if (nodelist != null && nodelist.getLength() != 0) {
            List<Parm> plist = new ArrayList<Parm>();
            for (int j = 0; j < nodelist.getLength(); j++) {
              Node parm = nodelist.item(j);
              // System.out.println(parm.getNodeName());
              Parm m = new Parm();
              Map<String, String> nMap = new HashMap<String, String>();
              Element elemm = (Element) parm;
              // System.out.println(elemm.getAttribute("name") +
              // elemm.getAttribute("value"));
              nMap.put("name", elemm.getAttribute("name"));
              nMap.put("value", elemm.getAttribute("value"));
              nMap.put("key", elemm.getAttribute("key"));
              m.setMap(nMap);
              plist.add(m);
            }
            server2.setName(name);
            server2.setPlist(plist);
            server2.setCommand(command);
            server2.setAddress(ip);
          } else {
            server2.setName(name);
            server2.setValue(value);
            server2.setCommand(command);
            server2.setAddress(ip);
            if (key != null || key != "") {
              server2.setKey(key);
            }
          }
          serverList.add(server2);
          if (StringUtils.isEmpty(command)) {
            command = "server" + (++serverNum);
          }
          serverTable.put(command, server2);
        }
      }
    }
    entityList.setServerList(serverList);
    return entityList;
  }

  public static Map<String, Server> getServerTable() {
    System.out.println(serverTable);
    return serverTable;
  }

  public static void setServerTable(Map<String, Server> serverTable) {
    ReadXml.serverTable = serverTable;
  }
}
