package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ChessRobotCfg;

/**
 * RobotAction_Chess.xlsx配置管理容器
 *
 * @excelName RobotAction_Chess.xlsx
 * @sheetName ChessRobot
 * @author auto_generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessRobotCfgContainer extends BaseCfgContainer<ChessRobotCfg> {

  @Override
  public boolean hasRelatedTable() {
    return true;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ChessRobotCfgContainer getNewContainer(){
    return new ChessRobotCfgContainer();
  }

  public ChessRobotCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("RobotAction_Chess.xlsx");
    return excelNameList;
  }

  @Override
  protected ChessRobotCfg createNewBean() {
    return new ChessRobotCfg();
  }
}
