package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ChessJackStrategyCfg;

/**
 * ChessJackStrategy.xlsx配置管理容器
 *
 * @excelName ChessJackStrategy.xlsx
 * @sheetName chessJackStrategy
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessJackStrategyCfgContainer extends BaseCfgContainer<ChessJackStrategyCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ChessJackStrategyCfgContainer getNewContainer(){
    return new ChessJackStrategyCfgContainer();
  }

  public ChessJackStrategyCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ChessJackStrategy.xlsx");
    return excelNameList;
  }

  @Override
  protected ChessJackStrategyCfg createNewBean() {
    return new ChessJackStrategyCfg();
  }
}
