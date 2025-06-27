package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.ShopConfigCfg;

/**
 * shop.xlsx配置管理容器
 *
 * @excelName shop.xlsx
 * @sheetName ShopConfig
 * @author auto_generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ShopConfigCfgContainer extends BaseCfgContainer<ShopConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ShopConfigCfgContainer getNewContainer(){
    return new ShopConfigCfgContainer();
  }

  public ShopConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("shop.xlsx");
    return excelNameList;
  }

  @Override
  protected ShopConfigCfg createNewBean() {
    return new ShopConfigCfg();
  }
}
