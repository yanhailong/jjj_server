package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorGenWatchVideoCfg;

/**
 * VisitorGenWatchVideo.xlsx配置管理容器
 *
 * @excelName VisitorGenWatchVideo.xlsx
 * @sheetName VisitorGenWatchVideo
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorGenWatchVideoCfgContainer extends BaseCfgContainer<VisitorGenWatchVideoCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorGenWatchVideoCfgContainer getNewContainer(){
    return new VisitorGenWatchVideoCfgContainer();
  }

  public VisitorGenWatchVideoCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorGenWatchVideo.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorGenWatchVideoCfg createNewBean() {
    return new VisitorGenWatchVideoCfg();
  }
}
