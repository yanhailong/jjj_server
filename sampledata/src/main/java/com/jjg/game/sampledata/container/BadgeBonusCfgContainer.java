package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BadgeBonusCfg;

/**
 * BadgeBonus.xlsx配置管理容器
 *
 * @excelName BadgeBonus.xlsx
 * @sheetName BadgeBonus
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BadgeBonusCfgContainer extends BaseCfgContainer<BadgeBonusCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BadgeBonusCfgContainer getNewContainer(){
    return new BadgeBonusCfgContainer();
  }

  public BadgeBonusCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BadgeBonus.xlsx");
    return excelNameList;
  }

  @Override
  protected BadgeBonusCfg createNewBean() {
    return new BadgeBonusCfg();
  }
}
