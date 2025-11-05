package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DailyRewardsCfg;

/**
 * DailyRewards.xlsx配置管理容器
 *
 * @excelName DailyRewards.xlsx
 * @sheetName DailyRewards
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DailyRewardsCfgContainer extends BaseCfgContainer<DailyRewardsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DailyRewardsCfgContainer getNewContainer(){
    return new DailyRewardsCfgContainer();
  }

  public DailyRewardsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("DailyRewards.xlsx");
    return excelNameList;
  }

  @Override
  protected DailyRewardsCfg createNewBean() {
    return new DailyRewardsCfg();
  }
}
