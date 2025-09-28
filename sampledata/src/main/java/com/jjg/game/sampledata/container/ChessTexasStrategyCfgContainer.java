package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ChessTexasStrategyCfg;

/**
 * chessTexasStrategy.xlsx配置管理容器
 *
 * @excelName chessTexasStrategy.xlsx
 * @sheetName chessTexasStrategy
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessTexasStrategyCfgContainer extends BaseCfgContainer<ChessTexasStrategyCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ChessTexasStrategyCfgContainer getNewContainer(){
    return new ChessTexasStrategyCfgContainer();
  }

  public ChessTexasStrategyCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("chessTexasStrategy.xlsx");
    return excelNameList;
  }

  @Override
  protected ChessTexasStrategyCfg createNewBean() {
    return new ChessTexasStrategyCfg();
  }
}
