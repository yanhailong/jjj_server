package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ActivePassCfg;

/**
 * ActivePass.xlsx配置管理容器
 *
 * @excelName ActivePass.xlsx
 * @sheetName ActivePass
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ActivePassCfgContainer extends BaseCfgContainer<ActivePassCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ActivePassCfgContainer getNewContainer(){
    return new ActivePassCfgContainer();
  }

  public ActivePassCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ActivePass.xlsx");
    return excelNameList;
  }

  @Override
  protected ActivePassCfg createNewBean() {
    return new ActivePassCfg();
  }
}
