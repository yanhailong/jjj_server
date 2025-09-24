package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PointsAwardRankingCfg;

/**
 * PointsAwardRanking.xlsx配置管理容器
 *
 * @excelName PointsAwardRanking.xlsx
 * @sheetName PointsAwardRanking
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardRankingCfgContainer extends BaseCfgContainer<PointsAwardRankingCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PointsAwardRankingCfgContainer getNewContainer(){
    return new PointsAwardRankingCfgContainer();
  }

  public PointsAwardRankingCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PointsAwardRanking.xlsx");
    return excelNameList;
  }

  @Override
  protected PointsAwardRankingCfg createNewBean() {
    return new PointsAwardRankingCfg();
  }
}
