package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonRankingCfg;

/**
 * SeasonRanking.xlsx配置管理容器
 *
 * @excelName SeasonRanking.xlsx
 * @sheetName SeasonRanking
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonRankingCfgContainer extends BaseCfgContainer<SeasonRankingCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonRankingCfgContainer getNewContainer(){
    return new SeasonRankingCfgContainer();
  }

  public SeasonRankingCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonRanking.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonRankingCfg createNewBean() {
    return new SeasonRankingCfg();
  }
}
