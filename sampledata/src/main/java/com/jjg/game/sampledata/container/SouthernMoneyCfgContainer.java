package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;

/**
 * SouthernMoney.xlsx配置管理容器
 *
 * @excelName SouthernMoney.xlsx
 * @sheetName SouthernMoney
 * @author auto_generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SouthernMoneyCfgContainer extends BaseCfgContainer<SouthernMoneyCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SouthernMoneyCfgContainer getNewContainer(){
    return new SouthernMoneyCfgContainer();
  }

  public SouthernMoneyCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SouthernMoney.xlsx");
    return excelNameList;
  }

  @Override
  protected SouthernMoneyCfg createNewBean() {
    return new SouthernMoneyCfg();
  }
}
