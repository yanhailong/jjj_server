package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonStartCfg;

/**
 * SeasonStart.xlsx配置管理容器
 *
 * @excelName SeasonStart.xlsx
 * @sheetName SeasonStart
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonStartCfgContainer extends BaseCfgContainer<SeasonStartCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonStartCfgContainer getNewContainer(){
    return new SeasonStartCfgContainer();
  }

  public SeasonStartCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonStart.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonStartCfg createNewBean() {
    return new SeasonStartCfg();
  }
}
