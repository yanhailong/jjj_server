package com.jjg.game.poker.game.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.poker.game.sample.bean.BlackjackCfg;

/**
 * Blackjack.xlsx配置管理容器
 *
 * @excelName Blackjack.xlsx
 * @sheetName Blackjack
 * @author auto_generator
 * @date 2025年08月04日 10:17:51
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BlackjackCfgContainer extends BaseCfgContainer<BlackjackCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BlackjackCfgContainer getNewContainer(){
    return new BlackjackCfgContainer();
  }

  public BlackjackCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Blackjack.xlsx");
    return excelNameList;
  }

  @Override
  protected BlackjackCfg createNewBean() {
    return new BlackjackCfg();
  }
}
