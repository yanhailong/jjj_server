package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DealerFunctionCfg;

/**
 * DealerFunction.xlsx配置管理容器
 *
 * @excelName DealerFunction.xlsx
 * @sheetName DealerFunction
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DealerFunctionCfgContainer extends BaseCfgContainer<DealerFunctionCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DealerFunctionCfgContainer getNewContainer(){
    return new DealerFunctionCfgContainer();
  }

  public DealerFunctionCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("DealerFunction.xlsx");
    return excelNameList;
  }

  @Override
  protected DealerFunctionCfg createNewBean() {
    return new DealerFunctionCfg();
  }
}
