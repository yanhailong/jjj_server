package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PokerPoolCfg;

/**
 * PokerPool.xlsx配置管理容器
 *
 * @excelName PokerPool.xlsx
 * @sheetName PokerPool
 * @author auto_generator
 * @date 2025年08月16日 15:49:31
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PokerPoolCfgContainer extends BaseCfgContainer<PokerPoolCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PokerPoolCfgContainer getNewContainer(){
    return new PokerPoolCfgContainer();
  }

  public PokerPoolCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PokerPool.xlsx");
    return excelNameList;
  }

  @Override
  protected PokerPoolCfg createNewBean() {
    return new PokerPoolCfg();
  }
}
