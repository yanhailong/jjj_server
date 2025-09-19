package com.jjg.game.sampledata.container;

import com.jjg.game.sampledata.bean.FirstpaymentCfg;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * FirstPayment.xlsx配置管理容器
 *
 * @excelName FirstPayment.xlsx
 * @sheetName firstpayment
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class FirstpaymentCfgContainer extends BaseCfgContainer<FirstpaymentCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public FirstpaymentCfgContainer getNewContainer(){
    return new FirstpaymentCfgContainer();
  }

  public FirstpaymentCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("FirstPayment.xlsx");
    return excelNameList;
  }

  @Override
  protected FirstpaymentCfg createNewBean() {
    return new FirstpaymentCfg();
  }
}
