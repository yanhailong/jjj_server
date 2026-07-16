package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MiningToolsCfg;

/**
 * MiningTools.xlsx配置管理容器
 *
 * @excelName MiningTools.xlsx
 * @sheetName MiningTools
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningToolsCfgContainer extends BaseCfgContainer<MiningToolsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MiningToolsCfgContainer getNewContainer(){
    return new MiningToolsCfgContainer();
  }

  public MiningToolsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MiningTools.xlsx");
    return excelNameList;
  }

  @Override
  protected MiningToolsCfg createNewBean() {
    return new MiningToolsCfg();
  }
}
