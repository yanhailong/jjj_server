package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PassListCfg;

/**
 * PassList.xlsx配置管理容器
 *
 * @excelName PassList.xlsx
 * @sheetName PassList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PassListCfgContainer extends BaseCfgContainer<PassListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PassListCfgContainer getNewContainer(){
    return new PassListCfgContainer();
  }

  public PassListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PassList.xlsx");
    return excelNameList;
  }

  @Override
  protected PassListCfg createNewBean() {
    return new PassListCfg();
  }
}
