package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.PlayerCfg;

/**
 * player.xlsx配置管理容器
 *
 * @excelName player.xlsx
 * @sheetName player
 * @author auto_generator
 * @date 2025年08月06日 20:26:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PlayerCfgContainer extends BaseCfgContainer<PlayerCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PlayerCfgContainer getNewContainer(){
    return new PlayerCfgContainer();
  }

  public PlayerCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("player.xlsx");
    return excelNameList;
  }

  @Override
  protected PlayerCfg createNewBean() {
    return new PlayerCfg();
  }
}
