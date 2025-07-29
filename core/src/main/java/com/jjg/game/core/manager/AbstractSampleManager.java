package com.jjg.game.core.manager;


import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.baselogic.DefaultCallback;
import com.jjg.game.common.monitor.FileLoader;
import com.jjg.game.common.monitor.FileMonitor;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.FileHelper;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Collection;
import java.util.List;
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
        try {
            //初始化excel配置表
            initSampleConfig();
            Map<String, ConfigExcelChangeListener> configExcelChangeListeners =
                CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
            // 调用配置表监听回调数据初始化逻辑
            configExcelChangeListeners.values().forEach(ConfigExcelChangeListener::initSampleCallbackCollector);
            // 初始化需要缓存的配置数据
            initLoadCacheData();
        } catch (Exception e) {
            log.error("配置表加载异常：{}", e.getMessage(), e);
            throw new GameSampleException(e.getMessage());
        }
        fileMonitor.addDirectoryObserver(getSamplePath(), this);

        //初始化游戏配置表，比如hallConfig.json , dollarExpressConfig.json
        if (!StringUtils.isEmpty(getGameConfigName())) {
            fileMonitor.addFileObserver(getGameConfigName(), this, true);
        }
    }

    /**
     * 启动时调用加载缓存配置数据的逻辑
     */
    private void initLoadCacheData() {
        Collection<File> sampleFile =
            FileUtils.listFiles(new File(getSamplePath()), new String[]{"xlsx", "xls"}, true);
        sampleFile.forEach(file -> load(file, false));
    }

    /**
     * 游戏excel配置所在目录
     */
    protected abstract String getSamplePath();

    /**
     * 初始化excel配置表
     */
    protected abstract void initSampleConfig() throws Exception;

    /**
     * excel变化时触发调用
     */
    protected abstract Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception;

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
                try {
                    // 加载配置文件成功后对应变化的CfgBean类
                    Set<Class<?>> changedSampleSet = reloadSampleOnExcelChange(file);
                    Map<String, ConfigExcelChangeListener> configExcelChangeListeners =
                        CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
                    List<Class<?>> changedSampleList = changedSampleSet.stream().toList();
                    for (ConfigExcelChangeListener listener : configExcelChangeListeners.values()) {
                        Map<String, DefaultCallback> callbackCollector = listener.getCallbackCollector();
                        if (callbackCollector.containsKey(fileName)) {
                            log.info("配置表文件：{} 变化，调用：{} 的重载逻辑",
                                fileName, listener.getClass().getSimpleName());
                            // 执行配置表变化监听回调
                            callbackCollector.get(fileName).run();
                        }
                    }
                    // 变化的配置文件列表
                    for (Class<?> changedSampleClass : changedSampleList) {
                        // 处理监听
                        for (ConfigExcelChangeListener listener : configExcelChangeListeners.values()) {
                            List<String> observeSampleFileList = listener.observeSampleFileList();
                            // 如果对应的文件listener需要处理配置文件
                            if (observeSampleFileList.contains(fileName)) {
                                listener.onSampleChange(changedSampleClass);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("重载配置表文件：{} 时发生异常", e.getMessage(), e);
                    throw new GameSampleException(e);
                }
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
