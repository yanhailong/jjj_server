package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AllianceShopCfg;

/**
 * AllianceShop.xlsx配置管理容器
 *
 * @excelName AllianceShop.xlsx
 * @sheetName AllianceShop
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllianceShopCfgContainer extends BaseCfgContainer<AllianceShopCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AllianceShopCfgContainer getNewContainer(){
    return new AllianceShopCfgContainer();
  }

  public AllianceShopCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AllianceShop.xlsx");
    return excelNameList;
  }

  @Override
  protected AllianceShopCfg createNewBean() {
    return new AllianceShopCfg();
  }
}
