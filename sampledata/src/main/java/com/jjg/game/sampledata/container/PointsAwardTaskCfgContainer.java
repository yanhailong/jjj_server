package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PointsAwardTaskCfg;

/**
 * PointsAwardTask.xlsx配置管理容器
 *
 * @excelName PointsAwardTask.xlsx
 * @sheetName PointsAwardTask
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardTaskCfgContainer extends BaseCfgContainer<PointsAwardTaskCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PointsAwardTaskCfgContainer getNewContainer(){
    return new PointsAwardTaskCfgContainer();
  }

  public PointsAwardTaskCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PointsAwardTask.xlsx");
    return excelNameList;
  }

  @Override
  protected PointsAwardTaskCfg createNewBean() {
    return new PointsAwardTaskCfg();
  }
}
