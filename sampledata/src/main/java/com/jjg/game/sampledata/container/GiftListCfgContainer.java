package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GiftListCfg;

/**
 * GiftList.xlsx配置管理容器
 *
 * @excelName GiftList.xlsx
 * @sheetName GiftList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GiftListCfgContainer extends BaseCfgContainer<GiftListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GiftListCfgContainer getNewContainer(){
    return new GiftListCfgContainer();
  }

  public GiftListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("GiftList.xlsx");
    return excelNameList;
  }

  @Override
  protected GiftListCfg createNewBean() {
    return new GiftListCfg();
  }
}
