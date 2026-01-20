package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.IponeAreacodeConfigCfg;

/**
 * iponeAreacodeConfig.xlsx配置管理容器
 *
 * @excelName iponeAreacodeConfig.xlsx
 * @sheetName iponeAreacodeConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class IponeAreacodeConfigCfgContainer extends BaseCfgContainer<IponeAreacodeConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public IponeAreacodeConfigCfgContainer getNewContainer(){
    return new IponeAreacodeConfigCfgContainer();
  }

  public IponeAreacodeConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("iponeAreacodeConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected IponeAreacodeConfigCfg createNewBean() {
    return new IponeAreacodeConfigCfg();
  }
}
