package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GuestStatsCfg;

/**
 * GuestStats.xlsx配置管理容器
 *
 * @excelName GuestStats.xlsx
 * @sheetName GuestStats
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuestStatsCfgContainer extends BaseCfgContainer<GuestStatsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GuestStatsCfgContainer getNewContainer(){
    return new GuestStatsCfgContainer();
  }

  public GuestStatsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("GuestStats.xlsx");
    return excelNameList;
  }

  @Override
  protected GuestStatsCfg createNewBean() {
    return new GuestStatsCfg();
  }
}
