package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MedalBuffCfg;

/**
 * MedalBuff.xlsx配置管理容器
 *
 * @excelName MedalBuff.xlsx
 * @sheetName MedalBuff
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MedalBuffCfgContainer extends BaseCfgContainer<MedalBuffCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MedalBuffCfgContainer getNewContainer(){
    return new MedalBuffCfgContainer();
  }

  public MedalBuffCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MedalBuff.xlsx");
    return excelNameList;
  }

  @Override
  protected MedalBuffCfg createNewBean() {
    return new MedalBuffCfg();
  }
}
