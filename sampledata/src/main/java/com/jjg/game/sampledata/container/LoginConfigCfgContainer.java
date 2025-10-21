package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.LoginConfigCfg;

/**
 * loginConfig.xlsx配置管理容器
 *
 * @excelName loginConfig.xlsx
 * @sheetName loginConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class LoginConfigCfgContainer extends BaseCfgContainer<LoginConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public LoginConfigCfgContainer getNewContainer(){
    return new LoginConfigCfgContainer();
  }

  public LoginConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("loginConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected LoginConfigCfg createNewBean() {
    return new LoginConfigCfg();
  }
}
