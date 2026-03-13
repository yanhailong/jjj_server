package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.UpcomingMobileGameCfg;

/**
 * UpcomingMobileGame.xlsx配置管理容器
 *
 * @excelName UpcomingMobileGame.xlsx
 * @sheetName UpcomingMobileGame
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class UpcomingMobileGameCfgContainer extends BaseCfgContainer<UpcomingMobileGameCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public UpcomingMobileGameCfgContainer getNewContainer(){
    return new UpcomingMobileGameCfgContainer();
  }

  public UpcomingMobileGameCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("UpcomingMobileGame.xlsx");
    return excelNameList;
  }

  @Override
  protected UpcomingMobileGameCfg createNewBean() {
    return new UpcomingMobileGameCfg();
  }
}
