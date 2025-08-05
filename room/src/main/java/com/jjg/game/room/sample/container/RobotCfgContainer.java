package com.jjg.game.room.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.room.sample.bean.RobotCfg;

/**
 * robot.xlsx配置管理容器
 *
 * @excelName robot.xlsx
 * @sheetName Robot
 * @author auto_generator
 * @date 2025年08月05日 10:45:15
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotCfgContainer extends BaseCfgContainer<RobotCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public RobotCfgContainer getNewContainer(){
    return new RobotCfgContainer();
  }

  public RobotCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("robot.xlsx");
    return excelNameList;
  }

  @Override
  protected RobotCfg createNewBean() {
    return new RobotCfg();
  }
}
