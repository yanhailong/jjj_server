package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.DefaultCallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/16 10:34
 */
public interface ConfigExcelChangeListener {

    Map<String, DefaultCallback> CALLBACK_COLLECTOR = new HashMap<>();

    /**
     * excel文件发生改变时调用
     */
    default void onSampleChange(Class<?> excelContainerClass) {
    }

    /**
     * 需要指定需要观察哪些文件
     */
    default List<String> observeSampleFileList() {
        return Collections.emptyList();
    }

    /**
     * 初始化回调收集器
     */
    default void initSampleCallbackCollector() {
    }

    /**
     * 获取配置表监听的回调收集器
     */
    default Map<String, DefaultCallback> getCallbackCollector() {
        return CALLBACK_COLLECTOR;
    }

    /**
     * 添加配置表监听的回调收集器
     */
    default ConfigExcelChangeListener addSampleFileObserveWithCallBack(String sampleName, DefaultCallback callback) {
        CALLBACK_COLLECTOR.put(sampleName, callback);
        return this;
    }

}
