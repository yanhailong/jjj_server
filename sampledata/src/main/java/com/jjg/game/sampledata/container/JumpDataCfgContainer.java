package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.JumpDataCfg;

/**
 * jumpData.xlsx配置管理容器
 *
 * @excelName jumpData.xlsx
 * @sheetName jumpData
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class JumpDataCfgContainer extends BaseCfgContainer<JumpDataCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public JumpDataCfgContainer getNewContainer(){
    return new JumpDataCfgContainer();
  }

  public JumpDataCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("jumpData.xlsx");
    return excelNameList;
  }

  @Override
  protected JumpDataCfg createNewBean() {
    return new JumpDataCfg();
  }
}
