package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryAwardCfg;

/**
 * SpecialAuxiliaryAward.xlsx配置管理容器
 *
 * @excelName SpecialAuxiliaryAward.xlsx
 * @sheetName SpecialAuxiliaryAward
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialAuxiliaryAwardCfgContainer extends BaseCfgContainer<SpecialAuxiliaryAwardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SpecialAuxiliaryAwardCfgContainer getNewContainer(){
    return new SpecialAuxiliaryAwardCfgContainer();
  }

  public SpecialAuxiliaryAwardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SpecialAuxiliaryAward.xlsx");
    return excelNameList;
  }

  @Override
  protected SpecialAuxiliaryAwardCfg createNewBean() {
    return new SpecialAuxiliaryAwardCfg();
  }
}
