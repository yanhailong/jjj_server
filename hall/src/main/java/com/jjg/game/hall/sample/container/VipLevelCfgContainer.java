package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.VipLevelCfg;

/**
 * vipLevel.xlsx配置管理容器
 *
 * @excelName vipLevel.xlsx
 * @sheetName VipLevel
 * @author auto_generator
 * @date 2025年07月18日 15:03:10
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VipLevelCfgContainer extends BaseCfgContainer<VipLevelCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VipLevelCfgContainer getNewContainer(){
    return new VipLevelCfgContainer();
  }

  public VipLevelCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("vipLevel.xlsx");
    return excelNameList;
  }

  @Override
  protected VipLevelCfg createNewBean() {
    return new VipLevelCfg();
  }
}
