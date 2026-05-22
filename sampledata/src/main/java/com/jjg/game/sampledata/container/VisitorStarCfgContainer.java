package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorStarCfg;

/**
 * VisitorStar.xlsx配置管理容器
 *
 * @excelName VisitorStar.xlsx
 * @sheetName VisitorStar
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorStarCfgContainer extends BaseCfgContainer<VisitorStarCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorStarCfgContainer getNewContainer(){
    return new VisitorStarCfgContainer();
  }

  public VisitorStarCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorStar.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorStarCfg createNewBean() {
    return new VisitorStarCfg();
  }
}
