package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MiningCellTypeCfg;

/**
 * MiningCellType.xlsx配置管理容器
 *
 * @excelName MiningCellType.xlsx
 * @sheetName MiningCellType
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningCellTypeCfgContainer extends BaseCfgContainer<MiningCellTypeCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MiningCellTypeCfgContainer getNewContainer(){
    return new MiningCellTypeCfgContainer();
  }

  public MiningCellTypeCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MiningCellType.xlsx");
    return excelNameList;
  }

  @Override
  protected MiningCellTypeCfg createNewBean() {
    return new MiningCellTypeCfg();
  }
}
