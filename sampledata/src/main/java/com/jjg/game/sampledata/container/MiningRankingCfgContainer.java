package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MiningRankingCfg;

/**
 * MiningRanking.xlsx配置管理容器
 *
 * @excelName MiningRanking.xlsx
 * @sheetName MiningRanking
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningRankingCfgContainer extends BaseCfgContainer<MiningRankingCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MiningRankingCfgContainer getNewContainer(){
    return new MiningRankingCfgContainer();
  }

  public MiningRankingCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MiningRanking.xlsx");
    return excelNameList;
  }

  @Override
  protected MiningRankingCfg createNewBean() {
    return new MiningRankingCfg();
  }
}
