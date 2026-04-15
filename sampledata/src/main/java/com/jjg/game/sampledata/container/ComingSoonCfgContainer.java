package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ComingSoonCfg;

/**
 * ComingSoon.xlsx配置管理容器
 *
 * @excelName ComingSoon.xlsx
 * @sheetName ComingSoon
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ComingSoonCfgContainer extends BaseCfgContainer<ComingSoonCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ComingSoonCfgContainer getNewContainer(){
    return new ComingSoonCfgContainer();
  }

  public ComingSoonCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ComingSoon.xlsx");
    return excelNameList;
  }

  @Override
  protected ComingSoonCfg createNewBean() {
    return new ComingSoonCfg();
  }
}
