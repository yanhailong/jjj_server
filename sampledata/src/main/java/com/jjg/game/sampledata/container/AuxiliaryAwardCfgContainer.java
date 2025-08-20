package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AuxiliaryAwardCfg;

/**
 * AuxiliaryAward.xlsx配置管理容器
 *
 * @excelName AuxiliaryAward.xlsx
 * @sheetName AuxiliaryAward
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AuxiliaryAwardCfgContainer extends BaseCfgContainer<AuxiliaryAwardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AuxiliaryAwardCfgContainer getNewContainer(){
    return new AuxiliaryAwardCfgContainer();
  }

  public AuxiliaryAwardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AuxiliaryAward.xlsx");
    return excelNameList;
  }

  @Override
  protected AuxiliaryAwardCfg createNewBean() {
    return new AuxiliaryAwardCfg();
  }
}
