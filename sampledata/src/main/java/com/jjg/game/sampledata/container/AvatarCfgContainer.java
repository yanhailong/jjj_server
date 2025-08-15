package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AvatarCfg;

/**
 * avatar.xlsx配置管理容器
 *
 * @excelName avatar.xlsx
 * @sheetName avatar
 * @author auto_generator
 * @date 2025年08月15日 18:30:10
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AvatarCfgContainer extends BaseCfgContainer<AvatarCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AvatarCfgContainer getNewContainer(){
    return new AvatarCfgContainer();
  }

  public AvatarCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("avatar.xlsx");
    return excelNameList;
  }

  @Override
  protected AvatarCfg createNewBean() {
    return new AvatarCfg();
  }
}
