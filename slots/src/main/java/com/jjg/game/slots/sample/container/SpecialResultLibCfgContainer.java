package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.SpecialResultLibCfg;

/**
 * SpecialResultLib.xlsx配置管理容器
 *
 * @excelName SpecialResultLib.xlsx
 * @sheetName SpecialResultLib
 * @author auto_generator
 * @date 2025年08月14日 19:04:34
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialResultLibCfgContainer extends BaseCfgContainer<SpecialResultLibCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SpecialResultLibCfgContainer getNewContainer(){
    return new SpecialResultLibCfgContainer();
  }

  public SpecialResultLibCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SpecialResultLib.xlsx");
    return excelNameList;
  }

  @Override
  protected SpecialResultLibCfg createNewBean() {
    return new SpecialResultLibCfg();
  }
}
