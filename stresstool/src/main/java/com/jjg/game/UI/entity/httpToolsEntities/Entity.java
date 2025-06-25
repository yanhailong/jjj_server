package com.jjg.game.UI.entity.httpToolsEntities;

import java.util.List;

/** 保存解析过后的XML文件 */
public class Entity {

  private List<Server> serverList;

  public List<Server> getSlist() {
    return serverList;
  }

  public void setServerList(List<Server> serverList) {
    this.serverList = serverList;
  }
}
