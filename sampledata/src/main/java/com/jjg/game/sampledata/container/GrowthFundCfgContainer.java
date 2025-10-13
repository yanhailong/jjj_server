package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GrowthFundCfg;

/**
 * GrowthFund.xlsx配置管理容器
 *
 * @excelName GrowthFund.xlsx
 * @sheetName GrowthFund
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GrowthFundCfgContainer extends BaseCfgContainer<GrowthFundCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GrowthFundCfgContainer getNewContainer(){
    return new GrowthFundCfgContainer();
  }

  public GrowthFundCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("GrowthFund.xlsx");
    return excelNameList;
  }

  @Override
  protected GrowthFundCfg createNewBean() {
    return new GrowthFundCfg();
  }
}
