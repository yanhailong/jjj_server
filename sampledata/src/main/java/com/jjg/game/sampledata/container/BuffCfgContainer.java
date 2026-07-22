package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuffCfg;

/**
 * Buff.xlsx配置管理容器
 *
 * @excelName Buff.xlsx
 * @sheetName Buff
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuffCfgContainer extends BaseCfgContainer<BuffCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuffCfgContainer getNewContainer(){
    return new BuffCfgContainer();
  }

  public BuffCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Buff.xlsx");
    return excelNameList;
  }

  @Override
  protected BuffCfg createNewBean() {
    return new BuffCfg();
  }
}
