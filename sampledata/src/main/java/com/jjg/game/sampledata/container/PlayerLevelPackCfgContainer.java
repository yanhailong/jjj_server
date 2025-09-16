package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PlayerLevelPackCfg;

/**
 * PlayerLevelPack.xlsx配置管理容器
 *
 * @excelName PlayerLevelPack.xlsx
 * @sheetName PlayerLevelPack
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PlayerLevelPackCfgContainer extends BaseCfgContainer<PlayerLevelPackCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PlayerLevelPackCfgContainer getNewContainer(){
    return new PlayerLevelPackCfgContainer();
  }

  public PlayerLevelPackCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PlayerLevelPack.xlsx");
    return excelNameList;
  }

  @Override
  protected PlayerLevelPackCfg createNewBean() {
    return new PlayerLevelPackCfg();
  }
}
