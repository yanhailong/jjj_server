package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ImmortalCardCfg;

/**
 * ImmortalCard.xlsx配置管理容器
 *
 * @excelName ImmortalCard.xlsx
 * @sheetName ImmortalCard
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ImmortalCardCfgContainer extends BaseCfgContainer<ImmortalCardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ImmortalCardCfgContainer getNewContainer(){
    return new ImmortalCardCfgContainer();
  }

  public ImmortalCardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ImmortalCard.xlsx");
    return excelNameList;
  }

  @Override
  protected ImmortalCardCfg createNewBean() {
    return new ImmortalCardCfg();
  }
}
