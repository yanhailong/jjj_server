package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.TaskCfg;

/**
 * task.xlsx配置管理容器
 *
 * @excelName task.xlsx
 * @sheetName task
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class TaskCfgContainer extends BaseCfgContainer<TaskCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public TaskCfgContainer getNewContainer(){
    return new TaskCfgContainer();
  }

  public TaskCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("task.xlsx");
    return excelNameList;
  }

  @Override
  protected TaskCfg createNewBean() {
    return new TaskCfg();
  }
}
