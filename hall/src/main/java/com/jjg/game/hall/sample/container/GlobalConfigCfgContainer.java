package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.GlobalConfigCfg;

/**
 * global.xlsx配置管理容器
 *
 * @excelName global.xlsx
 * @sheetName GlobalConfig
 * @author auto_generator
 * @date 2025年08月15日 15:43:07
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GlobalConfigCfgContainer extends BaseCfgContainer<GlobalConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GlobalConfigCfgContainer getNewContainer(){
    return new GlobalConfigCfgContainer();
  }

  public GlobalConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("global.xlsx");
    return excelNameList;
  }

  @Override
  protected GlobalConfigCfg createNewBean() {
    return new GlobalConfigCfg();
  }
}
