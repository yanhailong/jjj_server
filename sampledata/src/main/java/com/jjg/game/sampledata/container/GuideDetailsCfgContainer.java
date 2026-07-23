package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.GuideDetailsCfg;

/**
 * GuideDetails.xlsx配置管理容器
 *
 * @excelName GuideDetails.xlsx
 * @sheetName GuideDetails
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuideDetailsCfgContainer extends BaseCfgContainer<GuideDetailsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GuideDetailsCfgContainer getNewContainer(){
    return new GuideDetailsCfgContainer();
  }

  public GuideDetailsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("GuideDetails.xlsx");
    return excelNameList;
  }

  @Override
  protected GuideDetailsCfg createNewBean() {
    return new GuideDetailsCfg();
  }
}
