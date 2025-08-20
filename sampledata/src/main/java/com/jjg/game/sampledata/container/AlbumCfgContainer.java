package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AlbumCfg;

/**
 * album.xlsx配置管理容器
 *
 * @excelName album.xlsx
 * @sheetName album
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AlbumCfgContainer extends BaseCfgContainer<AlbumCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AlbumCfgContainer getNewContainer(){
    return new AlbumCfgContainer();
  }

  public AlbumCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("album.xlsx");
    return excelNameList;
  }

  @Override
  protected AlbumCfg createNewBean() {
    return new AlbumCfg();
  }
}
