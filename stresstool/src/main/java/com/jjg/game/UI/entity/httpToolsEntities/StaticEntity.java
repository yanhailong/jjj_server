package com.jjg.game.UI.entity.httpToolsEntities;

import java.util.HashMap;
import java.util.Map;

/** 数据修改后，保存的实体类 */
public class StaticEntity {

  private static StaticEntity staticEntity = new StaticEntity();

  private static Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();

  private Map<String, Map<String, String>> onMap = new HashMap<String, Map<String, String>>();

  private Map<String, String> commandMap = new HashMap<String, String>();

  public Map<String, Map<String, String>> getMap() {
    return map;
  }

  public static boolean setMap(Map<String, String> pmap, String name) {
    if (map.get(name) != null) {
      return false;
    } else {
      map.put(name, pmap);
      return true;
    }
  }

  public Map<String, Map<String, String>> getOnMap() {
    return onMap;
  }

  public void setOnMap(String name, Map<String, String> value) {
    onMap.put(name, value);
  }

  private StaticEntity() {}

  public Map<String, String> getCommandMap() {
    return commandMap;
  }

  public void setCommandMap(String name, String value) {
    commandMap.put(name, value);
  }

  public static StaticEntity getStaticEntity() {
    return staticEntity;
  }
}
