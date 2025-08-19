package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GameListCfg;

/**
 * gameList.xlsx配置管理容器
 *
 * @excelName gameList.xlsx
 * @sheetName GameList
 * @author auto_generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameListCfgContainer extends BaseCfgContainer<GameListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GameListCfgContainer getNewContainer(){
    return new GameListCfgContainer();
  }

  public GameListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("gameList.xlsx");
    return excelNameList;
  }

  @Override
  protected GameListCfg createNewBean() {
    return new GameListCfg();
  }
}
