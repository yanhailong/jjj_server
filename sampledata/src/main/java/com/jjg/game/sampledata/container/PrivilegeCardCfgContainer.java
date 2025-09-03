package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;

/**
 * PrivilegeCard.xlsx配置管理容器
 *
 * @excelName PrivilegeCard.xlsx
 * @sheetName PrivilegeCard
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PrivilegeCardCfgContainer extends BaseCfgContainer<PrivilegeCardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PrivilegeCardCfgContainer getNewContainer(){
    return new PrivilegeCardCfgContainer();
  }

  public PrivilegeCardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PrivilegeCard.xlsx");
    return excelNameList;
  }

  @Override
  protected PrivilegeCardCfg createNewBean() {
    return new PrivilegeCardCfg();
  }
}
