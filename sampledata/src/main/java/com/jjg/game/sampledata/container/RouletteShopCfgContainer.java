package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.RouletteShopCfg;

/**
 * RouletteShop.xlsx配置管理容器
 *
 * @excelName RouletteShop.xlsx
 * @sheetName RouletteShop
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RouletteShopCfgContainer extends BaseCfgContainer<RouletteShopCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public RouletteShopCfgContainer getNewContainer(){
    return new RouletteShopCfgContainer();
  }

  public RouletteShopCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("RouletteShop.xlsx");
    return excelNameList;
  }

  @Override
  protected RouletteShopCfg createNewBean() {
    return new RouletteShopCfg();
  }
}
