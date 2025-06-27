package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.GameListConfigCfg;

/**
 * gameList.xlsx配置管理容器
 *
 * @excelName gameList.xlsx
 * @sheetName GameListConfig
 * @author auto_generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameListConfigCfgContainer extends BaseCfgContainer<GameListConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GameListConfigCfgContainer getNewContainer(){
    return new GameListConfigCfgContainer();
  }

  public GameListConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("gameList.xlsx");
    return excelNameList;
  }

  @Override
  protected GameListConfigCfg createNewBean() {
    return new GameListConfigCfg();
  }
}
