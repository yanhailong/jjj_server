package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;

/**
 * CasinoStatsSheet.xlsx配置管理容器
 *
 * @excelName CasinoStatsSheet.xlsx
 * @sheetName CasinoStatsSheet
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CasinoStatsSheetCfgContainer extends BaseCfgContainer<CasinoStatsSheetCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public CasinoStatsSheetCfgContainer getNewContainer(){
    return new CasinoStatsSheetCfgContainer();
  }

  public CasinoStatsSheetCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("CasinoStatsSheet.xlsx");
    return excelNameList;
  }

  @Override
  protected CasinoStatsSheetCfg createNewBean() {
    return new CasinoStatsSheetCfg();
  }
}
