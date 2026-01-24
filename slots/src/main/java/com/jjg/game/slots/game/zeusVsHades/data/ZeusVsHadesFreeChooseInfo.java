package com.jjg.game.slots.game.zeusVsHades.data;

/**
 * @author lihaocao
 * @date 2025/9/10 9:42
 */
public class ZeusVsHadesFreeChooseInfo {
    //手动选择小游戏
    private int rewardId;
    //选择宙斯
    private int chooseZeus;
    //选择哈里斯
    private int chooseHades;

    public int getChooseZeus() {
        return chooseZeus;
    }

    public void setChooseZeus(int chooseZeus) {
        this.chooseZeus = chooseZeus;
    }

    public int getChooseHades() {
        return chooseHades;
    }

    public void setChooseHades(int chooseHades) {
        this.chooseHades = chooseHades;
    }

    public int getRewardId() {
        return rewardId;
    }

    public void setRewardId(int rewardId) {
        this.rewardId = rewardId;
    }

    public ZeusVsHadesFreeChooseInfo() {
    }
}
