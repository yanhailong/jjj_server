package com.jjg.game.common.utils;

import org.springframework.context.ApplicationContext;

/**
 * 存储ApplicationContext
 * @author 11
 * @date 2022/6/9
 */
public class CommonUtil {
    private static ApplicationContext context;

    public static ApplicationContext getContext() {
        return context;
    }

    public static void setContext(ApplicationContext context) {
        CommonUtil.context = context;
    }
}
