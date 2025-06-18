package com.jjg.game.common.monitor;

import java.io.File;

/**
 * 文件改变通知器
 *
 * @since 1.0
 */
public interface FileChangeListener {

    String getFileName();

    void onChange(File file);
}
