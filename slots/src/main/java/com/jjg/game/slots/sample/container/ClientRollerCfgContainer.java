package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.ClientRollerCfg;

/**
 * ClientRoller.xlsx配置管理容器
 *
 * @excelName ClientRoller.xlsx
 * @sheetName ClientRoller
 * @author auto_generator
 * @date 2025年08月02日 14:18:48
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ClientRollerCfgContainer extends BaseCfgContainer<ClientRollerCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ClientRollerCfgContainer getNewContainer(){
    return new ClientRollerCfgContainer();
  }

  public ClientRollerCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ClientRoller.xlsx");
    return excelNameList;
  }

  @Override
  protected ClientRollerCfg createNewBean() {
    return new ClientRollerCfg();
  }
}
