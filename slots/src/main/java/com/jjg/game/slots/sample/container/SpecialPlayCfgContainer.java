package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.SpecialPlayCfg;

/**
 * SpecialPlay.xlsx配置管理容器
 *
 * @excelName SpecialPlay.xlsx
 * @sheetName SpecialPlay
 * @author auto_generator
 * @date 2025年08月14日 19:04:34
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialPlayCfgContainer extends BaseCfgContainer<SpecialPlayCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SpecialPlayCfgContainer getNewContainer(){
    return new SpecialPlayCfgContainer();
  }

  public SpecialPlayCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SpecialPlay.xlsx");
    return excelNameList;
  }

  @Override
  protected SpecialPlayCfg createNewBean() {
    return new SpecialPlayCfg();
  }
}
