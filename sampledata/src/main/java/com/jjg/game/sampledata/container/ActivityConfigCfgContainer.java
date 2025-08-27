package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;

/**
 * ActivityConfig.xlsx配置管理容器
 *
 * @excelName ActivityConfig.xlsx
 * @sheetName ActivityConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ActivityConfigCfgContainer extends BaseCfgContainer<ActivityConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ActivityConfigCfgContainer getNewContainer(){
    return new ActivityConfigCfgContainer();
  }

  public ActivityConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ActivityConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected ActivityConfigCfg createNewBean() {
    return new ActivityConfigCfg();
  }
}
