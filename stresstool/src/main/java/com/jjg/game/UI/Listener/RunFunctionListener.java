package com.jjg.game.UI.Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.jjg.game.UI.window.frame.FunctionWindow;
import com.jjg.game.UI.window.frame.MainWindow;
import com.jjg.game.UI.window.frame.MessageWindow;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.robot.StressRobotManager;

/**
 * @author 2CL
 * @function 运行功能测试
 */
public class RunFunctionListener implements ActionListener {

    private MainWindow window;

    public RunFunctionListener(FunctionWindow window) {
        this.window = window;
    }

    public RunFunctionListener(MessageWindow window) {
        this.window = window;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Log4jManager.getInstance().info(this.window, "===============点击开始运行按钮====================");
        window.getRunButton().setEnabled(false);
        try {
            StressRobotManager.instance().start(window);
        } catch (Exception e) {
            Log4jManager.getInstance().error(window, e);
        }
    }
}
