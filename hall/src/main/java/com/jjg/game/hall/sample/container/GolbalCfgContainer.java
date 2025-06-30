package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.GolbalCfg;

/**
 * golbal.xlsx配置管理容器
 *
 * @excelName golbal.xlsx
 * @sheetName Golbal
 * @author auto_generator
 * @date 2025年06月30日 14:12:12
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GolbalCfgContainer extends BaseCfgContainer<GolbalCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GolbalCfgContainer getNewContainer(){
    return new GolbalCfgContainer();
  }

  public GolbalCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("golbal.xlsx");
    return excelNameList;
  }

  @Override
  protected GolbalCfg createNewBean() {
    return new GolbalCfg();
  }
}
