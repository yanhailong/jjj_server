package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.RoomCfg;

/**
 * Room_Bet.xlsx配置管理容器
 *
 * @excelName Room_Bet.xlsx
 * @sheetName Room
 * @author auto_generator
 * @date 2025年08月20日 13:34:25
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RoomCfgContainer extends BaseCfgContainer<RoomCfg> {

  @Override
  public boolean hasRelatedTable() {
    return true;
  }

  @Override
  public boolean isParentConfigNode() {
    return true;
  }

  @Override
  public RoomCfgContainer getNewContainer(){
    return new RoomCfgContainer();
  }

  public RoomCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Room_Bet.xlsx");
    excelNameList.add("Room_Chess.xlsx");
    return excelNameList;
  }

  @Override
  protected RoomCfg createNewBean() {
    return new RoomCfg();
  }
}
