package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import com.jjg.game.sampledata.bean.PoolResultsCfg;

/**
 * poolresults.xlsx配置管理容器
 *
 * @excelName poolresults.xlsx
 * @sheetName poolresults
 */
public class PoolResultsCfgContainer extends BaseCfgContainer<PoolResultsCfg> {

    @Override
    public boolean hasRelatedTable() {
        return false;
    }

    @Override
    public boolean isParentConfigNode() {
        return false;
    }

    @Override
    public PoolResultsCfgContainer getNewContainer() {
        return new PoolResultsCfgContainer();
    }

    public PoolResultsCfgContainer() {
        super();
    }

    @Override
    public List<String> getExcelNameList() {
        List<String> excelNameList = new ArrayList<>();
        excelNameList.add("poolresults.xlsx");
        return excelNameList;
    }

    @Override
    protected PoolResultsCfg createNewBean() {
        return new PoolResultsCfg();
    }
}
