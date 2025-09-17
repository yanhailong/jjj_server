package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MiniGameListCfg;

/**
 * MiniGameList.xlsx配置管理容器
 *
 * @excelName MiniGameList.xlsx
 * @sheetName MiniGameList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiniGameListCfgContainer extends BaseCfgContainer<MiniGameListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MiniGameListCfgContainer getNewContainer(){
    return new MiniGameListCfgContainer();
  }

  public MiniGameListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MiniGameList.xlsx");
    return excelNameList;
  }

  @Override
  protected MiniGameListCfg createNewBean() {
    return new MiniGameListCfg();
  }
}
