package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VideoRewardCfg;

/**
 * VideoReward.xlsx配置管理容器
 *
 * @excelName VideoReward.xlsx
 * @sheetName VideoReward
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VideoRewardCfgContainer extends BaseCfgContainer<VideoRewardCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VideoRewardCfgContainer getNewContainer(){
    return new VideoRewardCfgContainer();
  }

  public VideoRewardCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VideoReward.xlsx");
    return excelNameList;
  }

  @Override
  protected VideoRewardCfg createNewBean() {
    return new VideoRewardCfg();
  }
}
