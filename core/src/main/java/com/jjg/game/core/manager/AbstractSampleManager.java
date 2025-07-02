package com.jjg.game.core.manager;


import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.monitor.FileLoader;
import com.jjg.game.common.monitor.FileMonitor;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.FileHelper;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/6/30 12:42
 */
public abstract class AbstractSampleManager implements FileLoader {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private FileMonitor fileMonitor;

    /**
     * 初始化
     */
    public void init() {
        //初始化excel配置表
        initSampleConfig();
        fileMonitor.addDirectoryObserver(getSamplePath(), this);

        //初始化游戏配置表，比如hallConfig.json , dollarExpressConfig.json
        if (!StringUtils.isEmpty(getGameConfigName())) {
            fileMonitor.addFileObserver(getGameConfigName(), this, true);
        }
    }

    /**
     * 游戏excel配置所在目录
     */
    protected abstract String getSamplePath();

    /**
     * 初始化excel配置表
     */
    protected abstract void initSampleConfig();

    /**
     * TODO 配置表变化逻辑应提为公共方法，使用接口方式去通知哪些类进行了更新，否则每个子类都要去关注配置文件的变化(会产生很多重复代码)
     * TODO 而不对应的更新类的变化
     * excel变化
     *
     * @param file
     */
    protected abstract void sampleChange(File file);

    protected String getGameConfigName() {
        return null;
    }

    @Override
    public void load(File file, boolean isNew) {
        try {
            log.info("on file change filename = {},isNew={}", file.getName(), isNew);
            loadFile(file, true);
        } catch (Exception e) {
            log.warn("on file change err,filename={},isNew={}", file.getName(), isNew);
        }
    }

    /**
     * 加载所有的配置文件，包括xlsx和json
     *
     * @param file
     */
    public void loadFile(File file, boolean change) {
        if (file == null || !file.exists() || file.isHidden()
            || file.getName().endsWith(".svn")
            || file.getName().endsWith(".bak")
            || file.getName().startsWith("~$")) {
            return;
        }
        if (file.isDirectory()) {
            return;
        }

        String fileName = file.getName();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            if (change) {
                sampleChange(file);
            }
        } else if (fileName.endsWith(".json")) {
            loadJsonConfig(file);
        }
    }

    /**
     * 加载config目录下的json配置，类似 HallConfig.json
     *
     * @param file
     */
    private void loadJsonConfig(File file) {
        try {
            String fileName = file.getName();
            String content = FileHelper.readFile(file, GameConstant.Common.ENCODING);
            JSONObject jsonObject = JSONObject.parseObject(content);

            String name = fileName.replace(".json", "");
            Object bean = CommonUtil.getContext().getBean(name);
            BeanUtils.copyProperties(jsonObject.toJavaObject(bean.getClass()), bean);
        } catch (NoSuchBeanDefinitionException e1) {

        } catch (Exception e) {
            log.error("加载微服务配置失败 fileName = {}", file.getName(), e);
        }
    }
}
