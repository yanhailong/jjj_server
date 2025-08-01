package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.DefaultCallback;

import java.util.*;

/**
 * @author 11
 * @date 2025/6/16 10:34
 */
public interface ConfigExcelChangeListener {

    //初始化回调
    Map<String, List<DefaultCallback>> CALLBACK_COLLECTOR = new HashMap<>();
    //文件变化回调
    Map<String, List<DefaultCallback>> CHANGE_CALLBACK_COLLECTOR = new HashMap<>();

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
     * 初始化回调收集器
     */
    default void changeSampleCallbackCollector() {
    }

    /**
     * 获取配置表监听的回调收集器
     */
    default Map<String, List<DefaultCallback>> getCallbackCollector() {
        return CALLBACK_COLLECTOR;
    }

    /**
     * 获取配置表监听的回调收集器
     */
    default Map<String, List<DefaultCallback>> getChangeCallbackCollector() {
        return CHANGE_CALLBACK_COLLECTOR;
    }

    /**
     * 添加配置表监听的回调收集器
     */
    default ConfigExcelChangeListener addSampleFileObserveWithCallBack(String sampleName, DefaultCallback callback) {
        CALLBACK_COLLECTOR.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(callback);
        return this;
    }

    default ConfigExcelChangeListener addChangeSampleFileObserveWithCallBack(String sampleName, DefaultCallback callback) {
        CHANGE_CALLBACK_COLLECTOR.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(callback);
        return this;
    }

}
