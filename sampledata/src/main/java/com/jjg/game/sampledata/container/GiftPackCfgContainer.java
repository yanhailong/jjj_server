package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GiftPackCfg;

/**
 * GiftPack.xlsx配置管理容器
 *
 * @excelName GiftPack.xlsx
 * @sheetName GiftPack
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GiftPackCfgContainer extends BaseCfgContainer<GiftPackCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GiftPackCfgContainer getNewContainer(){
    return new GiftPackCfgContainer();
  }

  public GiftPackCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("GiftPack.xlsx");
    return excelNameList;
  }

  @Override
  protected GiftPackCfg createNewBean() {
    return new GiftPackCfg();
  }
}
