package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.FistpaymentCfg;

/**
 * FirstPayment.xlsx配置管理容器
 *
 * @excelName FirstPayment.xlsx
 * @sheetName fistpayment
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class FistpaymentCfgContainer extends BaseCfgContainer<FistpaymentCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public FistpaymentCfgContainer getNewContainer(){
    return new FistpaymentCfgContainer();
  }

  public FistpaymentCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("FirstPayment.xlsx");
    return excelNameList;
  }

  @Override
  protected FistpaymentCfg createNewBean() {
    return new FistpaymentCfg();
  }
}
