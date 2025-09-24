package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PointsAwardTurntableCfg;

/**
 * PointsAwardTurntable.xlsx配置管理容器
 *
 * @excelName PointsAwardTurntable.xlsx
 * @sheetName PointsAwardTurntable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardTurntableCfgContainer extends BaseCfgContainer<PointsAwardTurntableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PointsAwardTurntableCfgContainer getNewContainer(){
    return new PointsAwardTurntableCfgContainer();
  }

  public PointsAwardTurntableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PointsAwardTurntable.xlsx");
    return excelNameList;
  }

  @Override
  protected PointsAwardTurntableCfg createNewBean() {
    return new PointsAwardTurntableCfg();
  }
}
