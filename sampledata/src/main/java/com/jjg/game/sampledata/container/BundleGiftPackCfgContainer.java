package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BundleGiftPackCfg;

/**
 * BundleGiftPack.xlsx配置管理容器
 *
 * @excelName BundleGiftPack.xlsx
 * @sheetName BundleGiftPack
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BundleGiftPackCfgContainer extends BaseCfgContainer<BundleGiftPackCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BundleGiftPackCfgContainer getNewContainer(){
    return new BundleGiftPackCfgContainer();
  }

  public BundleGiftPackCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BundleGiftPack.xlsx");
    return excelNameList;
  }

  @Override
  protected BundleGiftPackCfg createNewBean() {
    return new BundleGiftPackCfg();
  }
}
