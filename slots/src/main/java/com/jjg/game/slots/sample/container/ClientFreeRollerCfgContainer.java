package com.jjg.game.slots.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.sample.bean.ClientFreeRollerCfg;

/**
 * ClientFreeRoller.xlsx配置管理容器
 *
 * @excelName ClientFreeRoller.xlsx
 * @sheetName ClientFreeRoller
 * @author auto_generator
 * @date 2025年07月30日 10:16:30
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ClientFreeRollerCfgContainer extends BaseCfgContainer<ClientFreeRollerCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ClientFreeRollerCfgContainer getNewContainer(){
    return new ClientFreeRollerCfgContainer();
  }

  public ClientFreeRollerCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ClientFreeRoller.xlsx");
    return excelNameList;
  }

  @Override
  protected ClientFreeRollerCfg createNewBean() {
    return new ClientFreeRollerCfg();
  }
}
