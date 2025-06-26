package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressGolbalConfigCfg;

/**
 * dollarExpressGolbal.xlsx配置管理容器
 *
 * @excelName dollarExpressGolbal.xlsx
 * @sheetName DollarExpressGolbalConfig
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressGolbalConfigCfgContainer extends BaseCfgContainer<DollarExpressGolbalConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressGolbalConfigCfgContainer getNewContainer(){
    return new DollarExpressGolbalConfigCfgContainer();
  }

  public DollarExpressGolbalConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressGolbal.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressGolbalConfigCfg createNewBean() {
    return new DollarExpressGolbalConfigCfg();
  }
}
