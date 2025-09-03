package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AccumulatedRewardsCfg;

/**
 * AccumulatedRewards.xlsx配置管理容器
 *
 * @excelName AccumulatedRewards.xlsx
 * @sheetName AccumulatedRewards
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AccumulatedRewardsCfgContainer extends BaseCfgContainer<AccumulatedRewardsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AccumulatedRewardsCfgContainer getNewContainer(){
    return new AccumulatedRewardsCfgContainer();
  }

  public AccumulatedRewardsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AccumulatedRewards.xlsx");
    return excelNameList;
  }

  @Override
  protected AccumulatedRewardsCfg createNewBean() {
    return new AccumulatedRewardsCfg();
  }
}
