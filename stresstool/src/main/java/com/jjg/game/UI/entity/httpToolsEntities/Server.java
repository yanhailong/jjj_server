package com.jjg.game.UI.entity.httpToolsEntities;

import java.util.List;

/** 保存命令的实体类 */
public class Server {

  private String name;
  private String value;
  private String command;
  private String address;
  private String spell;
  private String sport;

  public String getSport() {
    return sport;
  }

  public void setSport(String sport) {
    this.sport = sport;
  }

  public String getSpell() {
    return spell;
  }

  public void setSpell(String spell) {
    this.spell = spell;
  }

  private String key;
  private List<Parm> plist;

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Parm> getParamList() {
    return plist;
  }

  public void setPlist(List<Parm> plist) {
    this.plist = plist;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
