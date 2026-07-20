package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PassDetailsCfg;

/**
 * PassDetails.xlsx配置管理容器
 *
 * @excelName PassDetails.xlsx
 * @sheetName PassDetails
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PassDetailsCfgContainer extends BaseCfgContainer<PassDetailsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PassDetailsCfgContainer getNewContainer(){
    return new PassDetailsCfgContainer();
  }

  public PassDetailsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PassDetails.xlsx");
    return excelNameList;
  }

  @Override
  protected PassDetailsCfg createNewBean() {
    return new PassDetailsCfg();
  }
}
