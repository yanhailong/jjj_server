package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonTierCfg;

/**
 * SeasonTier.xlsx配置管理容器
 *
 * @excelName SeasonTier.xlsx
 * @sheetName SeasonTier
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonTierCfgContainer extends BaseCfgContainer<SeasonTierCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonTierCfgContainer getNewContainer(){
    return new SeasonTierCfgContainer();
  }

  public SeasonTierCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonTier.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonTierCfg createNewBean() {
    return new SeasonTierCfg();
  }
}
