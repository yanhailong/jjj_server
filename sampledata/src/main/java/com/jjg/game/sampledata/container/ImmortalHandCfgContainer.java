package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ImmortalHandCfg;

/**
 * ImmortalHand.xlsx配置管理容器
 *
 * @excelName ImmortalHand.xlsx
 * @sheetName ImmortalHand
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ImmortalHandCfgContainer extends BaseCfgContainer<ImmortalHandCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ImmortalHandCfgContainer getNewContainer(){
    return new ImmortalHandCfgContainer();
  }

  public ImmortalHandCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ImmortalHand.xlsx");
    return excelNameList;
  }

  @Override
  protected ImmortalHandCfg createNewBean() {
    return new ImmortalHandCfg();
  }
}
