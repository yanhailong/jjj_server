package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;

/**
 * WinPosWeight.xlsx配置管理容器
 *
 * @excelName WinPosWeight.xlsx
 * @sheetName WinPosWeight
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class WinPosWeightCfgContainer extends BaseCfgContainer<WinPosWeightCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public WinPosWeightCfgContainer getNewContainer(){
    return new WinPosWeightCfgContainer();
  }

  public WinPosWeightCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("WinPosWeight.xlsx");
    return excelNameList;
  }

  @Override
  protected WinPosWeightCfg createNewBean() {
    return new WinPosWeightCfg();
  }
}
