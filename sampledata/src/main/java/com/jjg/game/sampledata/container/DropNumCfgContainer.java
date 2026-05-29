package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DropNumCfg;

/**
 * dropNum.xlsx配置管理容器
 *
 * @excelName dropNum.xlsx
 * @sheetName dropNum
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropNumCfgContainer extends BaseCfgContainer<DropNumCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DropNumCfgContainer getNewContainer(){
    return new DropNumCfgContainer();
  }

  public DropNumCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dropNum.xlsx");
    return excelNameList;
  }

  @Override
  protected DropNumCfg createNewBean() {
    return new DropNumCfg();
  }
}
