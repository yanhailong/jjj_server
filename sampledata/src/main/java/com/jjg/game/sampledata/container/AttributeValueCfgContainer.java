package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AttributeValueCfg;

/**
 * AttributeValue.xlsx配置管理容器
 *
 * @excelName AttributeValue.xlsx
 * @sheetName AttributeValue
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AttributeValueCfgContainer extends BaseCfgContainer<AttributeValueCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AttributeValueCfgContainer getNewContainer(){
    return new AttributeValueCfgContainer();
  }

  public AttributeValueCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AttributeValue.xlsx");
    return excelNameList;
  }

  @Override
  protected AttributeValueCfg createNewBean() {
    return new AttributeValueCfg();
  }
}
