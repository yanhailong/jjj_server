package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonGemCraftCfg;

/**
 * SeasonGemCraft.xlsx配置管理容器
 *
 * @excelName SeasonGemCraft.xlsx
 * @sheetName SeasonGemCraft
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonGemCraftCfgContainer extends BaseCfgContainer<SeasonGemCraftCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonGemCraftCfgContainer getNewContainer(){
    return new SeasonGemCraftCfgContainer();
  }

  public SeasonGemCraftCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonGemCraft.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonGemCraftCfg createNewBean() {
    return new SeasonGemCraftCfg();
  }
}
