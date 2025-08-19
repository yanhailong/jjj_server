package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PoolCfg;

/**
 * Pool.xlsx配置管理容器
 *
 * @excelName Pool.xlsx
 * @sheetName Pool
 * @author auto_generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolCfgContainer extends BaseCfgContainer<PoolCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PoolCfgContainer getNewContainer(){
    return new PoolCfgContainer();
  }

  public PoolCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Pool.xlsx");
    return excelNameList;
  }

  @Override
  protected PoolCfg createNewBean() {
    return new PoolCfg();
  }
}
