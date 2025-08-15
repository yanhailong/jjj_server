package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GameFunctionCfg;

/**
 * GameFunction.xlsx配置管理容器
 *
 * @excelName GameFunction.xlsx
 * @sheetName GameFunction
 * @author auto_generator
 * @date 2025年08月15日 17:50:22
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameFunctionCfgContainer extends BaseCfgContainer<GameFunctionCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GameFunctionCfgContainer getNewContainer(){
    return new GameFunctionCfgContainer();
  }

  public GameFunctionCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("GameFunction.xlsx");
    return excelNameList;
  }

  @Override
  protected GameFunctionCfg createNewBean() {
    return new GameFunctionCfg();
  }
}
