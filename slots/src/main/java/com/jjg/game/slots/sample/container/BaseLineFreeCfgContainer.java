package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.BaseLineFreeCfg;

/**
 * BaseLineFree.xlsx配置管理容器
 *
 * @excelName BaseLineFree.xlsx
 * @sheetName BaseLineFree
 * @author auto_generator
 * @date 2025年07月22日 10:35:35
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
