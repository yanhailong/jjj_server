package com.jjg.game.core.tool;

import com.jjg.game.common.utils.CommonUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 控制台调用
 *
 * @author 2CL
 */
@Component
@Order()
public class ConsoleDebugger {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleDebugger.class);
    private static final String STOP_SERVER = "StopServer";
    /**
     * 是否已经从系统参数中读取
     */
    private static boolean IS_IDE_MODEL_FROM_SYSTEM_PROPERTY = false;
    private static boolean IS_IDE_MODEL = false;
    private static volatile boolean IS_STOPPING = false;
    private List<IConsoleReceiver> consoleReceivers = new ArrayList<>();

    @PostConstruct
    public boolean checkDebug() {
        if (IS_IDE_MODEL_FROM_SYSTEM_PROPERTY) {
            return IS_IDE_MODEL;
        }
        String val = System.getProperty("ideDebug");
        IS_IDE_MODEL = "true".equalsIgnoreCase(val);
        IS_IDE_MODEL_FROM_SYSTEM_PROPERTY = true;
        new Thread(this::checkConsole).start();
        return IS_IDE_MODEL;
    }

    public static boolean isIdeModel() {
        return IS_IDE_MODEL;
    }

    /**
     * 检查是否开启控制台
     */
    public void checkConsole() {
        if (isIdeModel()) {
            logger.info("IDE : Console Opened");
            Scanner scanner = new Scanner(System.in);
            while (!IS_STOPPING) {
                try {
                    String input;
                    if (scanner.hasNext()) {
                        input = scanner.next();
                        runCommand(input, scanner);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 执行命令
     */
    private void runCommand(String command, Scanner scanner) {
        // 进行预缓存
        if (consoleReceivers.isEmpty()) {
            Map<String, IConsoleReceiver> consoleReceiverMap =
                CommonUtil.getContext().getBeansOfType(IConsoleReceiver.class);
            consoleReceivers = new ArrayList<>(consoleReceiverMap.values());
        }
        List<String> params = new ArrayList<>();
        params.add(scanner.nextLine());
        boolean hasHandleCommand = false;
        // 执行指令
        for (IConsoleReceiver consoleReceiver : consoleReceivers) {
            if (consoleReceiver.needHandleCommands().stream().anyMatch(s -> s.equalsIgnoreCase(command))) {
                logger.info("IDE : Running command: {}", command);
                consoleReceiver.doCommand(command, params);
                hasHandleCommand = true;
            }
        }
        if ("SysGc".equalsIgnoreCase(command)) {
            System.gc();
        } else if (command.equalsIgnoreCase(STOP_SERVER)) {
            IS_STOPPING = true;
            SpringApplication.exit(CommonUtil.getContext());
        } else if (!hasHandleCommand) {
            logger.info("不能识别的命令: {}", command);
        }
    }
}
