package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.RobotActionCfg;

/**
 * RobotAction_Bet.xlsx配置管理容器
 *
 * @excelName RobotAction_Bet.xlsx
 * @sheetName RobotAction
 * @author auto_generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotActionCfgContainer extends BaseCfgContainer<RobotActionCfg> {

  @Override
  public boolean hasRelatedTable() {
    return true;
  }

  @Override
  public boolean isParentConfigNode() {
    return true;
  }

  @Override
  public RobotActionCfgContainer getNewContainer(){
    return new RobotActionCfgContainer();
  }

  public RobotActionCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("RobotAction_Bet.xlsx");
    excelNameList.add("RobotAction_Chess.xlsx");
    return excelNameList;
  }

  @Override
  protected RobotActionCfg createNewBean() {
    return new RobotActionCfg();
  }
}
