package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.Room_BetCfg;

/**
 * Room_Bet.xlsx配置管理容器
 *
 * @excelName Room_Bet.xlsx
 * @sheetName Room_Bet
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class Room_BetCfgContainer extends BaseCfgContainer<Room_BetCfg> {

  @Override
  public boolean hasRelatedTable() {
    return true;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public Room_BetCfgContainer getNewContainer(){
    return new Room_BetCfgContainer();
  }

  public Room_BetCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Room_Bet.xlsx");
    return excelNameList;
  }

  @Override
  protected Room_BetCfg createNewBean() {
    return new Room_BetCfg();
  }
}
