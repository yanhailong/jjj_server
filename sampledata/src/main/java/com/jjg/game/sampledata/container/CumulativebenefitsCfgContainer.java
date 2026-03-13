package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.CumulativebenefitsCfg;

/**
 * Cumulativebenefits.xlsx配置管理容器
 *
 * @excelName Cumulativebenefits.xlsx
 * @sheetName Cumulativebenefits
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CumulativebenefitsCfgContainer extends BaseCfgContainer<CumulativebenefitsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public CumulativebenefitsCfgContainer getNewContainer(){
    return new CumulativebenefitsCfgContainer();
  }

  public CumulativebenefitsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Cumulativebenefits.xlsx");
    return excelNameList;
  }

  @Override
  protected CumulativebenefitsCfg createNewBean() {
    return new CumulativebenefitsCfg();
  }
}
