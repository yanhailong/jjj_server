package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PlayerLevelConfigCfg;

/**
 * playerLevelConfig.xlsx配置管理容器
 *
 * @excelName playerLevelConfig.xlsx
 * @sheetName playerLevelConfig
 * @author auto_generator
 * @date 2025年08月19日 15:29:43
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PlayerLevelConfigCfgContainer extends BaseCfgContainer<PlayerLevelConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PlayerLevelConfigCfgContainer getNewContainer(){
    return new PlayerLevelConfigCfgContainer();
  }

  public PlayerLevelConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("playerLevelConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected PlayerLevelConfigCfg createNewBean() {
    return new PlayerLevelConfigCfg();
  }
}
