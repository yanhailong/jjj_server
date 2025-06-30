package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.ShopCfg;

/**
 * shop.xlsx配置管理容器
 *
 * @excelName shop.xlsx
 * @sheetName Shop
 * @author auto_generator
 * @date 2025年06月30日 14:12:12
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ShopCfgContainer extends BaseCfgContainer<ShopCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ShopCfgContainer getNewContainer(){
    return new ShopCfgContainer();
  }

  public ShopCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("shop.xlsx");
    return excelNameList;
  }

  @Override
  protected ShopCfg createNewBean() {
    return new ShopCfg();
  }
}
