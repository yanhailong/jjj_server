package com.jjg.game.core.listener;

import java.io.File;

/**
 * @author Administrator
 */
public interface GameSampleFileChangeListener {
    /**
     * 当excel文件发生变化时调用，用于触发配置表加载逻辑
     *
     * @param changedExcelConfig 变化的配置表文件
     */
    void change(File changedExcelConfig);
}
