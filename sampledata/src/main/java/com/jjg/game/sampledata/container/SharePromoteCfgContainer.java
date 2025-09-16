package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SharePromoteCfg;

/**
 * SharePromote.xlsx配置管理容器
 *
 * @excelName SharePromote.xlsx
 * @sheetName SharePromote
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SharePromoteCfgContainer extends BaseCfgContainer<SharePromoteCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SharePromoteCfgContainer getNewContainer(){
    return new SharePromoteCfgContainer();
  }

  public SharePromoteCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SharePromote.xlsx");
    return excelNameList;
  }

  @Override
  protected SharePromoteCfg createNewBean() {
    return new SharePromoteCfg();
  }
}
