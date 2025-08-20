package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BaseLineCfg;

/**
 * BaseLine.xlsx配置管理容器
 *
 * @excelName BaseLine.xlsx
 * @sheetName BaseLine
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseLineCfgContainer extends BaseCfgContainer<BaseLineCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseLineCfgContainer getNewContainer(){
    return new BaseLineCfgContainer();
  }

  public BaseLineCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseLine.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseLineCfg createNewBean() {
    return new BaseLineCfg();
  }
}
