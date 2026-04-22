package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.RunninglightCfg;

/**
 * runninglight.xlsx配置管理容器
 *
 * @excelName runninglight.xlsx
 * @sheetName runninglight
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RunninglightCfgContainer extends BaseCfgContainer<RunninglightCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public RunninglightCfgContainer getNewContainer(){
    return new RunninglightCfgContainer();
  }

  public RunninglightCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("runninglight.xlsx");
    return excelNameList;
  }

  @Override
  protected RunninglightCfg createNewBean() {
    return new RunninglightCfg();
  }
}
