package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.BaseRoomCfg;

/**
 * BaseRoom.xlsx配置管理容器
 *
 * @excelName BaseRoom.xlsx
 * @sheetName BaseRoom
 * @author auto_generator
 * @date 2025年07月22日 10:35:35
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRoomCfgContainer extends BaseCfgContainer<BaseRoomCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseRoomCfgContainer getNewContainer(){
    return new BaseRoomCfgContainer();
  }

  public BaseRoomCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseRoom.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseRoomCfg createNewBean() {
    return new BaseRoomCfg();
  }
}
