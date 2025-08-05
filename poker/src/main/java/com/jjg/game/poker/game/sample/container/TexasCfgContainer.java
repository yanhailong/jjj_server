package com.jjg.game.poker.game.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.poker.game.sample.bean.TexasCfg;

/**
 * Texas.xlsx配置管理容器
 *
 * @excelName Texas.xlsx
 * @sheetName Texas
 * @author auto_generator
 * @date 2025年08月04日 10:17:51
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class TexasCfgContainer extends BaseCfgContainer<TexasCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public TexasCfgContainer getNewContainer(){
    return new TexasCfgContainer();
  }

  public TexasCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Texas.xlsx");
    return excelNameList;
  }

  @Override
  protected TexasCfg createNewBean() {
    return new TexasCfg();
  }
}
