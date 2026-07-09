package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonGemDropCfg;

/**
 * SeasonGemDrop.xlsx配置管理容器
 *
 * @excelName SeasonGemDrop.xlsx
 * @sheetName SeasonGemDrop
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonGemDropCfgContainer extends BaseCfgContainer<SeasonGemDropCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonGemDropCfgContainer getNewContainer(){
    return new SeasonGemDropCfgContainer();
  }

  public SeasonGemDropCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonGemDrop.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonGemDropCfg createNewBean() {
    return new SeasonGemDropCfg();
  }
}
