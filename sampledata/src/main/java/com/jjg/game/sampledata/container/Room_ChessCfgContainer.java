package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

/**
 * Room_Chess.xlsx配置管理容器
 *
 * @excelName Room_Chess.xlsx
 * @sheetName Room_Chess
 * @author auto_generator
 * @date 2025年08月19日 15:29:42
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class Room_ChessCfgContainer extends BaseCfgContainer<Room_ChessCfg> {

  @Override
  public boolean hasRelatedTable() {
    return true;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public Room_ChessCfgContainer getNewContainer(){
    return new Room_ChessCfgContainer();
  }

  public Room_ChessCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Room_Chess.xlsx");
    return excelNameList;
  }

  @Override
  protected Room_ChessCfg createNewBean() {
    return new Room_ChessCfg();
  }
}
