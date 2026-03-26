package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;

/**
 * PloygameRoom.xlsx配置管理容器
 *
 * @excelName PloygameRoom.xlsx
 * @sheetName PloygameRoom
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PloygameRoomCfgContainer extends BaseCfgContainer<PloygameRoomCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PloygameRoomCfgContainer getNewContainer(){
    return new PloygameRoomCfgContainer();
  }

  public PloygameRoomCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PloygameRoom.xlsx");
    return excelNameList;
  }

  @Override
  protected PloygameRoomCfg createNewBean() {
    return new PloygameRoomCfg();
  }
}
