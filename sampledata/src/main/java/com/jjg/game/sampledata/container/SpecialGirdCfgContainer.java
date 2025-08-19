package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SpecialGirdCfg;

/**
 * SpecialGird.xlsx配置管理容器
 *
 * @excelName SpecialGird.xlsx
 * @sheetName SpecialGird
 * @author auto_generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialGirdCfgContainer extends BaseCfgContainer<SpecialGirdCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SpecialGirdCfgContainer getNewContainer(){
    return new SpecialGirdCfgContainer();
  }

  public SpecialGirdCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SpecialGird.xlsx");
    return excelNameList;
  }

  @Override
  protected SpecialGirdCfg createNewBean() {
    return new SpecialGirdCfg();
  }
}
