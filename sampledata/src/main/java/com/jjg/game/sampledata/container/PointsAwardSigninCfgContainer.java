package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PointsAwardSigninCfg;

/**
 * PointsAwardSignin.xlsx配置管理容器
 *
 * @excelName PointsAwardSignin.xlsx
 * @sheetName PointsAwardSignin
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardSigninCfgContainer extends BaseCfgContainer<PointsAwardSigninCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PointsAwardSigninCfgContainer getNewContainer(){
    return new PointsAwardSigninCfgContainer();
  }

  public PointsAwardSigninCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PointsAwardSignin.xlsx");
    return excelNameList;
  }

  @Override
  protected PointsAwardSigninCfg createNewBean() {
    return new PointsAwardSigninCfg();
  }
}
