package com.jjg.game.reddot;

import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.service.PackRedDotService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackRedDotTest {
    @Test void countsUsableGiftGridsNotStackSizeAndClearsAtZero() {
        PackRedDotService service = new PackRedDotService();
        PlayerPackService packs = mock(PlayerPackService.class);
        PlayerPack pack = new PlayerPack(1);
        pack.getItems().putAll(Map.of(1, new Item(11, 100), 2, new Item(11, 2), 3, new Item(12, 99), 4, new Item(11, 0)));
        when(packs.getFromAllDB(1)).thenReturn(pack);
        ReflectionTestUtils.setField(service, "packs", packs);
        ReflectionTestUtils.setField(service, "manager", new RedDotManager(null, null, null));
        ItemCfg gift = mock(ItemCfg.class), material = mock(ItemCfg.class);
        when(gift.getType()).thenReturn(GameConstant.Item.TYPE_CAN_USE);
        when(gift.getGetItem()).thenReturn(Map.of(100, 1L));
        try (MockedStatic<GameDataManager> configs = mockStatic(GameDataManager.class)) {
            configs.when(() -> GameDataManager.getItemCfg(11)).thenReturn(gift);
            configs.when(() -> GameDataManager.getItemCfg(12)).thenReturn(material);
            assertEquals(2, service.initialize(1, 0).getFirst().getCount());
            assertTrue(service.initialize(1, 1).getFirst().getExtra().contains("[1,2]"));
            pack.getItems().clear();
            assertEquals(0, service.initialize(1, 0).getFirst().getCount());
        }
        verify(packs, never()).removeItems(any(), anyMap(), any(), anyString());
    }
}
