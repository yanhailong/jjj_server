package com.jjg.game.common.utils;

/**
 * 异常堆栈打印
 *
 * @author 2CL
 */
public class ExceptionUtils {
    /**
     * 错误堆栈的内容
     *
     * @param e e
     * @return e
     */
    private static String e2sOneNoThread(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e);
        Throwable throwable = e.getCause();
        if (throwable != null) {
            String message = throwable.getMessage();
            if (message != null) {
                sb.append(" : ");
                sb.append(message);
            }
        }
        return sb.toString();
    }

    public static String e2sOne(Exception e) {
        StringBuilder sb = new StringBuilder();
        String eStr = e2sOneNoThread(e);
        sb.append(eStr);
        sb.append("\n");
        sb.append("threadName:").append(Thread.currentThread().getName());
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 错误堆栈的内容
     */
    public static String e2s(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e2sOneNoThread(e));
        sb.append("\n");
        for (StackTraceElement ste : e.getStackTrace()) {
            sb.append("at ");
            sb.append(ste);
            sb.append("\n");
        }
        sb.append("threadName:").append(Thread.currentThread().getName());
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 打印当前线程堆栈信息
     */
    public static String currentThreadTracesWithOutMineLine() {
        return currentThreadTraces(2, false);
    }

    /**
     * 打印当前线程堆栈信息
     */
    public static String currentThreadTraces(int len) {
        return currentThreadTraces(len, false);
    }

    /**
     * 打印当前线程堆栈信息
     */
    public static String currentThreadTracesSingleOne(int len) {
        return currentThreadTraces(len, true);
    }

    /**
     * 打印当前线程堆栈信息
     */
    public static String currentThreadTraces() {
        return currentThreadTraces(1);
    }

    /**
     * 打印当前线程堆栈信息
     */
    public static String currentThreadTraces(int len, boolean isOne) {
        Thread thread = Thread.currentThread();

        StringBuilder sb = new StringBuilder();

        sb.append("java.lang.threadTraces.Exception : thread id: ")
            .append(thread.getId())
            .append(", name: ")
            .append(thread.getName())
            .append(", ")
            .append(thread.getState())
            .append("\n");
        StackTraceElement[] stackTraces = thread.getStackTrace();
        int totalLen = stackTraces.length;
        int beginLen = 2 + len;
        if (totalLen < (beginLen + 1)) {
            return "";
        }

        if (isOne) {
            totalLen = beginLen + 1;
        }

        for (int i = beginLen; i < totalLen; i++) {
            sb.append("at ");
            sb.append(stackTraces[i]);
            sb.append("\n");
        }
        return sb.toString();
    }
}
