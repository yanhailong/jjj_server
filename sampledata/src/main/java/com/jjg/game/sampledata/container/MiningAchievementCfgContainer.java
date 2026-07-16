package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MiningAchievementCfg;

/**
 * MiningAchievement.xlsx配置管理容器
 *
 * @excelName MiningAchievement.xlsx
 * @sheetName MiningAchievement
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningAchievementCfgContainer extends BaseCfgContainer<MiningAchievementCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MiningAchievementCfgContainer getNewContainer(){
    return new MiningAchievementCfgContainer();
  }

  public MiningAchievementCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MiningAchievement.xlsx");
    return excelNameList;
  }

  @Override
  protected MiningAchievementCfg createNewBean() {
    return new MiningAchievementCfg();
  }
}
