package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.StatusCfg;

/**
 * status.xlsx配置管理容器
 *
 * @excelName status.xlsx
 * @sheetName status
 * @author auto_generator
 * @date 2025年08月15日 15:43:07
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class StatusCfgContainer extends BaseCfgContainer<StatusCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public StatusCfgContainer getNewContainer(){
    return new StatusCfgContainer();
  }

  public StatusCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("status.xlsx");
    return excelNameList;
  }

  @Override
  protected StatusCfg createNewBean() {
    return new StatusCfg();
  }
}
