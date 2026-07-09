package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonShopCfg;

/**
 * SeasonShop.xlsx配置管理容器
 *
 * @excelName SeasonShop.xlsx
 * @sheetName SeasonShop
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonShopCfgContainer extends BaseCfgContainer<SeasonShopCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonShopCfgContainer getNewContainer(){
    return new SeasonShopCfgContainer();
  }

  public SeasonShopCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonShop.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonShopCfg createNewBean() {
    return new SeasonShopCfg();
  }
}
