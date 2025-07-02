package com.jjg.game.table.baccarat.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.table.baccarat.sample.bean.BetAreaCfg;

/**
 * BetArea.xlsx配置管理容器
 *
 * @excelName BetArea.xlsx
 * @sheetName BetArea
 * @author auto_generator
 * @date 2025年07月18日 09:48:33
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
