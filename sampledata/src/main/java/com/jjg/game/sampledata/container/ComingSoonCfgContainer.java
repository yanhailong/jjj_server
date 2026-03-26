package com.jjg.game.sampledata.container;

import com.jjg.game.sampledata.bean.ComingSoonCfg;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

/**
 * ComingSoon.xlsx配置管理容器
 *
 * @author auto_generator
 * @excelName ComingSoon.xlsx
 * @sheetName ComingSoon
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ComingSoonCfgContainer extends BaseCfgContainer<ComingSoonCfg> {

    public ComingSoonCfgContainer() {
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
    public ComingSoonCfgContainer getNewContainer() {
        return new ComingSoonCfgContainer();
    }

    @Override
    public List<String> getExcelNameList() {
        List<String> excelNameList = new ArrayList<>();
        excelNameList.add("ComingSoon.xlsx");
        return excelNameList;
    }

    @Override
    protected ComingSoonCfg createNewBean() {
        return new ComingSoonCfg();
    }
}
