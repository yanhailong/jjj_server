package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.BaseElementRewardCfg;

/**
 * BaseElementReward.xlsx配置管理容器
 *
 * @excelName BaseElementReward.xlsx
 * @sheetName BaseElementReward
 * @author auto_generator
 * @date 2025年08月14日 19:04:33
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseElementRewardCfgContainer extends BaseCfgContainer<BaseElementRewardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseElementRewardCfgContainer getNewContainer(){
    return new BaseElementRewardCfgContainer();
  }

  public BaseElementRewardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseElementReward.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseElementRewardCfg createNewBean() {
    return new BaseElementRewardCfg();
  }
}
