package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.DollarExpressMessageHandler;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.pb.ReqChooseFreeModel;
import com.jjg.game.slots.game.dollarexpress.pb.ReqStartGame;
import com.jjg.game.slots.service.SlotsPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * @author 11
 * @date 2025/7/19 15:34
 */
@Component
public class DollarExpressTestManager {

    @Autowired
    private DollarExpressMessageHandler messageHandler;
    @Autowired
    private DollarExpressGameManager gameManager;
    @Autowired
    private SlotsPlayerService slotsPlayerService;

    public void init(){
        testScanner();
    }


    private void testScanner(){
        try{
            Player player = slotsPlayerService.get(1000001);
            player.setGameType(100100);
            player.setRoomCfgId(1001001);

            PlayerController playerController = new PlayerController(null,player);
            DollarExpressPlayerGameData playerGameData = gameManager.createPlayerGameData(playerController);
            playerGameData.setLastModelId(4);

            Scanner scanner = new Scanner(System.in);
            while (true){
                System.out.println("请输入命令：");
                String str = scanner.nextLine();
                String[] arr = str.split("\\s+");
                messageHandler.gm(playerController,arr);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
