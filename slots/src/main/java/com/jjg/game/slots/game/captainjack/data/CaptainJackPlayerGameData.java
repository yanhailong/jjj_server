package com.jjg.game.slots.game.captainjack.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackPlayerGameDataDTO;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
public class CaptainJackPlayerGameData extends SlotsPlayerGameData {
    //寻宝次数
    private AtomicInteger alreadyDigCount = new AtomicInteger(0);
    //当前依赖的寻宝libId
    private CaptainJackResultLib resultLib;

    public int addAlreadyDigCount(int count) {
        if (alreadyDigCount == null) {
            alreadyDigCount = new AtomicInteger(0);
        }
        return alreadyDigCount.addAndGet(count);
    }

    public int getAlreadyDigCount() {
        return alreadyDigCount == null ? 0 : alreadyDigCount.get();
    }

    public void setAlreadyDigCount(AtomicInteger alreadyDigCount) {
        this.alreadyDigCount = alreadyDigCount;
    }

    public CaptainJackResultLib getResultLib() {
        return resultLib;
    }

    public void setResultLib(CaptainJackResultLib resultLib) {
        this.resultLib = resultLib;
    }

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof CaptainJackPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setAlreadyDigCount(this.alreadyDigCount == null ? 0 : this.alreadyDigCount.get());
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            gameDataDTO.setResultLib(this.resultLib);
            if (this.freeLib instanceof CaptainJackResultLib lib) {
                gameDataDTO.setFreeLib(lib);
            }
        }
        return dto;
    }
}
