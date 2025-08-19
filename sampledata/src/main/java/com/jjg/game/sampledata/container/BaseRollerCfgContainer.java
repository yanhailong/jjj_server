package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BaseRollerCfg;

/**
 * BaseRoller.xlsx配置管理容器
 *
 * @excelName BaseRoller.xlsx
 * @sheetName BaseRoller
 * @author auto_generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRollerCfgContainer extends BaseCfgContainer<BaseRollerCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseRollerCfgContainer getNewContainer(){
    return new BaseRollerCfgContainer();
  }

  public BaseRollerCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseRoller.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseRollerCfg createNewBean() {
    return new BaseRollerCfg();
  }
}
