package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ViplevelCfg;

/**
 * viplevel.xlsx配置管理容器
 *
 * @excelName viplevel.xlsx
 * @sheetName viplevel
 * @author auto_generator
 * @date 2025年08月19日 15:29:43
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ViplevelCfgContainer extends BaseCfgContainer<ViplevelCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ViplevelCfgContainer getNewContainer(){
    return new ViplevelCfgContainer();
  }

  public ViplevelCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("viplevel.xlsx");
    return excelNameList;
  }

  @Override
  protected ViplevelCfg createNewBean() {
    return new ViplevelCfg();
  }
}
