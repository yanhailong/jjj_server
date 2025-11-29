package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.WealthRouletteRewardCfg;

/**
 * WealthRouletteReward.xlsx配置管理容器
 *
 * @excelName WealthRouletteReward.xlsx
 * @sheetName WealthRouletteReward
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class WealthRouletteRewardCfgContainer extends BaseCfgContainer<WealthRouletteRewardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public WealthRouletteRewardCfgContainer getNewContainer(){
    return new WealthRouletteRewardCfgContainer();
  }

  public WealthRouletteRewardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("WealthRouletteReward.xlsx");
    return excelNameList;
  }

  @Override
  protected WealthRouletteRewardCfg createNewBean() {
    return new WealthRouletteRewardCfg();
  }
}
