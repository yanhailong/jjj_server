package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AllianceLevelCfg;

/**
 * AllianceLevel.xlsx配置管理容器
 *
 * @excelName AllianceLevel.xlsx
 * @sheetName AllianceLevel
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllianceLevelCfgContainer extends BaseCfgContainer<AllianceLevelCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AllianceLevelCfgContainer getNewContainer(){
    return new AllianceLevelCfgContainer();
  }

  public AllianceLevelCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AllianceLevel.xlsx");
    return excelNameList;
  }

  @Override
  protected AllianceLevelCfg createNewBean() {
    return new AllianceLevelCfg();
  }
}
