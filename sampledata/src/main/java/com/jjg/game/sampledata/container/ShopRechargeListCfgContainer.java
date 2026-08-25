package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;

/**
 * ShopRechargeList.xlsx配置管理容器
 *
 * @excelName ShopRechargeList.xlsx
 * @sheetName ShopRechargeList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ShopRechargeListCfgContainer extends BaseCfgContainer<ShopRechargeListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ShopRechargeListCfgContainer getNewContainer(){
    return new ShopRechargeListCfgContainer();
  }

  public ShopRechargeListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ShopRechargeList.xlsx");
    return excelNameList;
  }

  @Override
  protected ShopRechargeListCfg createNewBean() {
    return new ShopRechargeListCfg();
  }
}
