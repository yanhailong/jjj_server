package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.Sheet1Cfg;

/**
 * condition.xlsx配置管理容器
 *
 * @excelName condition.xlsx
 * @sheetName Sheet1
 * @author auto_generator
 * @date 2025年08月15日 15:43:07
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class Sheet1CfgContainer extends BaseCfgContainer<Sheet1Cfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public Sheet1CfgContainer getNewContainer(){
    return new Sheet1CfgContainer();
  }

  public Sheet1CfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("condition.xlsx");
    return excelNameList;
  }

  @Override
  protected Sheet1Cfg createNewBean() {
    return new Sheet1Cfg();
  }
}
