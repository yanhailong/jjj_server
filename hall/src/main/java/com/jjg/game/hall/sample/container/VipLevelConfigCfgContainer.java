package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.VipLevelConfigCfg;

/**
 * vipLevel.xlsx配置管理容器
 *
 * @excelName vipLevel.xlsx
 * @sheetName VipLevelConfig
 * @author auto_generator
 * @date 2025年08月08日 13:44:57
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VipLevelConfigCfgContainer extends BaseCfgContainer<VipLevelConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VipLevelConfigCfgContainer getNewContainer(){
    return new VipLevelConfigCfgContainer();
  }

  public VipLevelConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("vipLevel.xlsx");
    return excelNameList;
  }

  @Override
  protected VipLevelConfigCfg createNewBean() {
    return new VipLevelConfigCfg();
  }
}
