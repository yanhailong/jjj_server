package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.OfficialAwardsCfg;

/**
 * OfficialAwards.xlsx配置管理容器
 *
 * @excelName OfficialAwards.xlsx
 * @sheetName OfficialAwards
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class OfficialAwardsCfgContainer extends BaseCfgContainer<OfficialAwardsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public OfficialAwardsCfgContainer getNewContainer(){
    return new OfficialAwardsCfgContainer();
  }

  public OfficialAwardsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("OfficialAwards.xlsx");
    return excelNameList;
  }

  @Override
  protected OfficialAwardsCfg createNewBean() {
    return new OfficialAwardsCfg();
  }
}
