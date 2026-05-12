package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AirstrikeRobotCfg;

/**
 * AirstrikeRobot‌.xlsx配置管理容器
 *
 * @excelName AirstrikeRobot‌.xlsx
 * @sheetName AirstrikeRobot‌
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AirstrikeRobotCfgContainer extends BaseCfgContainer<AirstrikeRobotCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AirstrikeRobotCfgContainer getNewContainer(){
    return new AirstrikeRobotCfgContainer();
  }

  public AirstrikeRobotCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AirstrikeRobot‌.xlsx");
    return excelNameList;
  }

  @Override
  protected AirstrikeRobotCfg createNewBean() {
    return new AirstrikeRobotCfg();
  }
}
