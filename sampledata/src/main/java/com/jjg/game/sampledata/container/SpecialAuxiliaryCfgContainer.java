package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;

/**
 * SpecialAuxiliary.xlsx配置管理容器
 *
 * @excelName SpecialAuxiliary.xlsx
 * @sheetName SpecialAuxiliary
 * @author auto_generator
 * @date 2025年08月16日 15:49:31
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialAuxiliaryCfgContainer extends BaseCfgContainer<SpecialAuxiliaryCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SpecialAuxiliaryCfgContainer getNewContainer(){
    return new SpecialAuxiliaryCfgContainer();
  }

  public SpecialAuxiliaryCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SpecialAuxiliary.xlsx");
    return excelNameList;
  }

  @Override
  protected SpecialAuxiliaryCfg createNewBean() {
    return new SpecialAuxiliaryCfg();
  }
}
