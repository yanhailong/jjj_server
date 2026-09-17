package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PassRewardCfg;

/**
 * PassReward.xlsx配置管理容器
 *
 * @excelName PassReward.xlsx
 * @sheetName PassReward
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PassRewardCfgContainer extends BaseCfgContainer<PassRewardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PassRewardCfgContainer getNewContainer(){
    return new PassRewardCfgContainer();
  }

  public PassRewardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PassReward.xlsx");
    return excelNameList;
  }

  @Override
  protected PassRewardCfg createNewBean() {
    return new PassRewardCfg();
  }
}
