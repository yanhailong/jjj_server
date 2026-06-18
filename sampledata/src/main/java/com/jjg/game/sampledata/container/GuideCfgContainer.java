package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GuideCfg;

/**
 * Guide.xlsx配置管理容器
 *
 * @excelName Guide.xlsx
 * @sheetName Guide
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuideCfgContainer extends BaseCfgContainer<GuideCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GuideCfgContainer getNewContainer(){
    return new GuideCfgContainer();
  }

  public GuideCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Guide.xlsx");
    return excelNameList;
  }

  @Override
  protected GuideCfg createNewBean() {
    return new GuideCfg();
  }
}
