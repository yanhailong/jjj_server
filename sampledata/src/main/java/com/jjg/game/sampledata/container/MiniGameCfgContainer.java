package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MiniGameCfg;

/**
 * MiniGame.xlsx配置管理容器
 *
 * @excelName MiniGame.xlsx
 * @sheetName MiniGame
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiniGameCfgContainer extends BaseCfgContainer<MiniGameCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MiniGameCfgContainer getNewContainer(){
    return new MiniGameCfgContainer();
  }

  public MiniGameCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MiniGame.xlsx");
    return excelNameList;
  }

  @Override
  protected MiniGameCfg createNewBean() {
    return new MiniGameCfg();
  }
}
