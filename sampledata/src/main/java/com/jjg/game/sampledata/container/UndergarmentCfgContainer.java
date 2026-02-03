package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.UndergarmentCfg;

/**
 * undergarment.xlsx配置管理容器
 *
 * @excelName undergarment.xlsx
 * @sheetName undergarment
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class UndergarmentCfgContainer extends BaseCfgContainer<UndergarmentCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public UndergarmentCfgContainer getNewContainer(){
    return new UndergarmentCfgContainer();
  }

  public UndergarmentCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("undergarment.xlsx");
    return excelNameList;
  }

  @Override
  protected UndergarmentCfg createNewBean() {
    return new UndergarmentCfg();
  }
}
