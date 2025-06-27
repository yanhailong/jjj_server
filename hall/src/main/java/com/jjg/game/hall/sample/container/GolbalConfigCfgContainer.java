package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.GolbalConfigCfg;

/**
 * golbal.xlsx配置管理容器
 *
 * @excelName golbal.xlsx
 * @sheetName GolbalConfig
 * @author auto_generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GolbalConfigCfgContainer extends BaseCfgContainer<GolbalConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public GolbalConfigCfgContainer getNewContainer(){
    return new GolbalConfigCfgContainer();
  }

  public GolbalConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("golbal.xlsx");
    return excelNameList;
  }

  @Override
  protected GolbalConfigCfg createNewBean() {
    return new GolbalConfigCfg();
  }
}
