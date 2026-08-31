package com.jjg.game.sim.service;

import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.sim.dao.SimVisitDao;
import com.jjg.game.sim.data.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimVisitRedDotTest {
    @Test void onlyOwnerAndActualPageAreMarkedRead() {
        SimVisitService service = new SimVisitService();
        SimVisitDao dao = mock(SimVisitDao.class);
        SimVisitConfigService config = mock(SimVisitConfigService.class); when(config.getRecordLimit()).thenReturn(100);
        SimVisitProfileData profile = new SimVisitProfileData(); profile.setUnreadCommentCount(3);
        List<SimVisitCommentData> comments = new ArrayList<>();
        for (String id : List.of("a", "b", "c")) { var c = new SimVisitCommentData(); c.setId(id); comments.add(c); }
        profile.setComments(comments);
        when(dao.findCommentsView(anyLong())).thenReturn(profile); when(dao.findBrief(anyLong())).thenReturn(profile);
        RedDotManager dots = spy(new RedDotManager(null, null, null)); doNothing().when(dots).updateRedDot(anyList(), anyLong());
        ReflectionTestUtils.setField(service, "visitDao", dao); ReflectionTestUtils.setField(service, "configService", config);
        ReflectionTestUtils.setField(service, "quotaService", mock(SimVisitQuotaService.class)); ReflectionTestUtils.setField(service, "redDotManager", dots);
        SimPlayerContext ctx = mock(SimPlayerContext.class); when(ctx.playerId()).thenReturn(1L);
        var response = service.comments(ctx, 1, 1);
        assertEquals(1, response.comments.size()); assertEquals("b", response.comments.getFirst().id);
        verify(dao).markCommentsRead(1, List.of("b"));
        service.comments(ctx, 99, 10);
        when(ctx.getVisitTargetId()).thenReturn(2L); service.comments(ctx, 0, 10);
        verify(dao, times(1)).markCommentsRead(anyLong(), anyCollection());
    }
}
