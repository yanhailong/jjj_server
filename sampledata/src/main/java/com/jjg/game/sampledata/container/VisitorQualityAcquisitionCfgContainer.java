package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorQualityAcquisitionCfg;

/**
 * VisitorQualityAcquisition.xlsx配置管理容器
 *
 * @excelName VisitorQualityAcquisition.xlsx
 * @sheetName VisitorQualityAcquisition
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorQualityAcquisitionCfgContainer extends BaseCfgContainer<VisitorQualityAcquisitionCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorQualityAcquisitionCfgContainer getNewContainer(){
    return new VisitorQualityAcquisitionCfgContainer();
  }

  public VisitorQualityAcquisitionCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorQualityAcquisition.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorQualityAcquisitionCfg createNewBean() {
    return new VisitorQualityAcquisitionCfg();
  }
}
