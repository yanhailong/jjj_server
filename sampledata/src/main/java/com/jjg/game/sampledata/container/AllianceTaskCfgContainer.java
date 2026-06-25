package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AllianceTaskCfg;

/**
 * AllianceTask.xlsx配置管理容器
 *
 * @excelName AllianceTask.xlsx
 * @sheetName AllianceTask
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllianceTaskCfgContainer extends BaseCfgContainer<AllianceTaskCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AllianceTaskCfgContainer getNewContainer(){
    return new AllianceTaskCfgContainer();
  }

  public AllianceTaskCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AllianceTask.xlsx");
    return excelNameList;
  }

  @Override
  protected AllianceTaskCfg createNewBean() {
    return new AllianceTaskCfg();
  }
}
