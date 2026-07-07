package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PopularityRankingCfg;

/**
 * PopularityRanking.xlsx配置管理容器
 *
 * @excelName PopularityRanking.xlsx
 * @sheetName PopularityRanking
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PopularityRankingCfgContainer extends BaseCfgContainer<PopularityRankingCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PopularityRankingCfgContainer getNewContainer(){
    return new PopularityRankingCfgContainer();
  }

  public PopularityRankingCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PopularityRanking.xlsx");
    return excelNameList;
  }

  @Override
  protected PopularityRankingCfg createNewBean() {
    return new PopularityRankingCfg();
  }
}
