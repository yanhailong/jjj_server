package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BaseLineFreeCfg;

/**
 * BaseLineFree.xlsx配置管理容器
 *
 * @excelName BaseLineFree.xlsx
 * @sheetName BaseLineFree
 * @author auto_generator
 * @date 2025年08月15日 17:50:22
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseLineFreeCfgContainer extends BaseCfgContainer<BaseLineFreeCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BaseLineFreeCfgContainer getNewContainer(){
    return new BaseLineFreeCfgContainer();
  }

  public BaseLineFreeCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BaseLineFree.xlsx");
    return excelNameList;
  }

  @Override
  protected BaseLineFreeCfg createNewBean() {
    return new BaseLineFreeCfg();
  }
}
