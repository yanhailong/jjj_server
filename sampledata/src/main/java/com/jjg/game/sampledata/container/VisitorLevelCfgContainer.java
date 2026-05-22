package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorLevelCfg;

/**
 * VisitorLevel.xlsx配置管理容器
 *
 * @excelName VisitorLevel.xlsx
 * @sheetName VisitorLevel
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorLevelCfgContainer extends BaseCfgContainer<VisitorLevelCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorLevelCfgContainer getNewContainer(){
    return new VisitorLevelCfgContainer();
  }

  public VisitorLevelCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorLevel.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorLevelCfg createNewBean() {
    return new VisitorLevelCfg();
  }
}
