package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BetAreaCfg;

/**
 * BetArea.xlsx配置管理容器
 *
 * @excelName BetArea.xlsx
 * @sheetName BetArea
 * @author auto_generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BetAreaCfgContainer extends BaseCfgContainer<BetAreaCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BetAreaCfgContainer getNewContainer(){
    return new BetAreaCfgContainer();
  }

  public BetAreaCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BetArea.xlsx");
    return excelNameList;
  }

  @Override
  protected BetAreaCfg createNewBean() {
    return new BetAreaCfg();
  }
}
