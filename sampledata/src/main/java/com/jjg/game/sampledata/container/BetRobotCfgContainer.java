package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BetRobotCfg;

/**
 * RobotAction_Bet.xlsx配置管理容器
 *
 * @excelName RobotAction_Bet.xlsx
 * @sheetName BetRobot
 * @author auto_generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BetRobotCfgContainer extends BaseCfgContainer<BetRobotCfg> {

  @Override
  public boolean hasRelatedTable() {
    return true;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BetRobotCfgContainer getNewContainer(){
    return new BetRobotCfgContainer();
  }

  public BetRobotCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("RobotAction_Bet.xlsx");
    return excelNameList;
  }

  @Override
  protected BetRobotCfg createNewBean() {
    return new BetRobotCfg();
  }
}
