package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PopUpConfigCfg;

/**
 * popUpConfig.xlsx配置管理容器
 *
 * @excelName popUpConfig.xlsx
 * @sheetName popUpConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PopUpConfigCfgContainer extends BaseCfgContainer<PopUpConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PopUpConfigCfgContainer getNewContainer(){
    return new PopUpConfigCfgContainer();
  }

  public PopUpConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("popUpConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected PopUpConfigCfg createNewBean() {
    return new PopUpConfigCfg();
  }
}
