package com.jjg.game.sampledata.container;

import com.jjg.game.sampledata.bean.AirRaidCfg;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * AirRaid.xlsx配置管理容器
 *
 * @author auto_generator
 * @excelName AirRaid.xlsx
 * @sheetName AirRaid
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AirRaidCfgContainer extends BaseCfgContainer<AirRaidCfg> {

    public AirRaidCfgContainer() {
        super();
    }

    @Override
    public boolean hasRelatedTable() {
        return false;
    }

    @Override
    public boolean isParentConfigNode() {
        return false;
    }

    @Override
    public AirRaidCfgContainer getNewContainer() {
        return new AirRaidCfgContainer();
    }

    @Override
    public List<String> getExcelNameList() {
        List<String> excelNameList = new ArrayList<>();
        excelNameList.add("AirRaid.xlsx");
        return excelNameList;
    }

    @Override
    protected AirRaidCfg createNewBean() {
        return new AirRaidCfg();
    }
}
