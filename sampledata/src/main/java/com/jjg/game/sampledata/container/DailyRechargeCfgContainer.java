package com.jjg.game.sampledata.container;

import com.jjg.game.sampledata.bean.DailyRechargeCfg;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * DailyRecharge.xlsx配置管理容器
 *
 * @excelName DailyRecharge.xlsx
 * @sheetName DailyRecharge
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DailyRechargeCfgContainer extends BaseCfgContainer<DailyRechargeCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DailyRechargeCfgContainer getNewContainer(){
    return new DailyRechargeCfgContainer();
  }

  public DailyRechargeCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("DailyRecharge.xlsx");
    return excelNameList;
  }

  @Override
  protected DailyRechargeCfg createNewBean() {
    return new DailyRechargeCfg();
  }
}
