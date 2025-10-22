package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PopUpGetWayCfg;

/**
 * popUpGetWay.xlsx配置管理容器
 *
 * @excelName popUpGetWay.xlsx
 * @sheetName popUpGetWay
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PopUpGetWayCfgContainer extends BaseCfgContainer<PopUpGetWayCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PopUpGetWayCfgContainer getNewContainer(){
    return new PopUpGetWayCfgContainer();
  }

  public PopUpGetWayCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("popUpGetWay.xlsx");
    return excelNameList;
  }

  @Override
  protected PopUpGetWayCfg createNewBean() {
    return new PopUpGetWayCfg();
  }
}
