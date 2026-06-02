package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MedalListCfg;

/**
 * MedalList.xlsx配置管理容器
 *
 * @excelName MedalList.xlsx
 * @sheetName MedalList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MedalListCfgContainer extends BaseCfgContainer<MedalListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MedalListCfgContainer getNewContainer(){
    return new MedalListCfgContainer();
  }

  public MedalListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MedalList.xlsx");
    return excelNameList;
  }

  @Override
  protected MedalListCfg createNewBean() {
    return new MedalListCfg();
  }
}
