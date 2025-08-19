package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BaseElementCfg;

/**
 * BaseElement.xlsx配置管理容器
 *
 * @excelName BaseElement.xlsx
 * @sheetName BaseElement
 * @author auto_generator
 * @date 2025年08月19日 15:29:43
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseElementCfgContainer extends BaseCfgContainer<BaseElementCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseElementCfgContainer getNewContainer(){
    return new BaseElementCfgContainer();
  }

  public BaseElementCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseElement.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseElementCfg createNewBean() {
    return new BaseElementCfg();
  }
}
