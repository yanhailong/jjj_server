package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasondropDetailedCfg;

/**
 * SeasondropDetailed.xlsx配置管理容器
 *
 * @excelName SeasondropDetailed.xlsx
 * @sheetName SeasondropDetailed
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasondropDetailedCfgContainer extends BaseCfgContainer<SeasondropDetailedCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasondropDetailedCfgContainer getNewContainer(){
    return new SeasondropDetailedCfgContainer();
  }

  public SeasondropDetailedCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasondropDetailed.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasondropDetailedCfg createNewBean() {
    return new SeasondropDetailedCfg();
  }
}
