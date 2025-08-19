package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.RoomExpendCfg;

/**
 * RoomExpend.xlsx配置管理容器
 *
 * @excelName RoomExpend.xlsx
 * @sheetName RoomExpend
 * @author auto_generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RoomExpendCfgContainer extends BaseCfgContainer<RoomExpendCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public RoomExpendCfgContainer getNewContainer(){
    return new RoomExpendCfgContainer();
  }

  public RoomExpendCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("RoomExpend.xlsx");
    return excelNameList;
  }

  @Override
  protected RoomExpendCfg createNewBean() {
    return new RoomExpendCfg();
  }
}
