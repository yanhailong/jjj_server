package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PointsAwardRobotCfg;

/**
 * PointsAwardRobot.xlsx配置管理容器
 *
 * @excelName PointsAwardRobot.xlsx
 * @sheetName PointsAwardRobot
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardRobotCfgContainer extends BaseCfgContainer<PointsAwardRobotCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PointsAwardRobotCfgContainer getNewContainer(){
    return new PointsAwardRobotCfgContainer();
  }

  public PointsAwardRobotCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PointsAwardRobot.xlsx");
    return excelNameList;
  }

  @Override
  protected PointsAwardRobotCfg createNewBean() {
    return new PointsAwardRobotCfg();
  }
}
