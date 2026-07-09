package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonMatchCfg;

/**
 * SeasonMatch.xlsx配置管理容器
 *
 * @excelName SeasonMatch.xlsx
 * @sheetName SeasonMatch
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonMatchCfgContainer extends BaseCfgContainer<SeasonMatchCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonMatchCfgContainer getNewContainer(){
    return new SeasonMatchCfgContainer();
  }

  public SeasonMatchCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonMatch.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonMatchCfg createNewBean() {
    return new SeasonMatchCfg();
  }
}
