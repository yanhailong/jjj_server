package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BaseRollerModeCfg;

/**
 * BaseRollerMode.xlsx配置管理容器
 *
 * @excelName BaseRollerMode.xlsx
 * @sheetName BaseRollerMode
 * @author auto_generator
 * @date 2025年08月15日 17:50:22
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRollerModeCfgContainer extends BaseCfgContainer<BaseRollerModeCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseRollerModeCfgContainer getNewContainer(){
    return new BaseRollerModeCfgContainer();
  }

  public BaseRollerModeCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseRollerMode.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseRollerModeCfg createNewBean() {
    return new BaseRollerModeCfg();
  }
}
