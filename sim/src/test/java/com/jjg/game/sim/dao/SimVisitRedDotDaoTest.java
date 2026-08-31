package com.jjg.game.sim.dao;

import com.jjg.game.sim.data.SimVisitCommentData;
import com.jjg.game.sim.data.SimVisitProfileData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimVisitRedDotDaoTest {
    private SimVisitCommentData comment(String id, Boolean unread) {
        SimVisitCommentData c = new SimVisitCommentData(); c.setId(id); c.setUnread(unread); return c;
    }
    @Test void legacyUnreadMigratesAndExplicitReadIsPreserved() {
        var comments = List.of(comment("a", null), comment("b", false), comment("c", null));
        SimVisitDao.normalizeUnread(comments, 2);
        assertTrue(comments.get(0).getUnread());
        assertFalse(comments.get(1).getUnread());
        assertFalse(comments.get(2).getUnread());
    }

    @Test void pageReadRetriesOnConcurrentCommentWithoutClearingIt() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        SimVisitDao dao = mock(SimVisitDao.class, CALLS_REAL_METHODS);
        org.springframework.test.util.ReflectionTestUtils.setField(dao, "mongoTemplate", mongo);
        SimVisitProfileData before = new SimVisitProfileData(), after = new SimVisitProfileData();
        before.setCommentRevision(1L); before.setUnreadCommentCount(2);
        before.setComments(List.of(comment("a", true), comment("b", true)));
        after.setCommentRevision(2L); after.setUnreadCommentCount(3);
        after.setComments(List.of(comment("new", true), comment("a", true), comment("b", true)));
        when(mongo.findOne(any(Query.class), eq(SimVisitProfileData.class))).thenReturn(before, after);
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(SimVisitProfileData.class)))
                .thenReturn(null, after);
        dao.markCommentsRead(1, List.of("a"));
        ArgumentCaptor<Update> updates = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2)).findAndModify(any(Query.class), updates.capture(), any(FindAndModifyOptions.class), eq(SimVisitProfileData.class));
        org.bson.Document set = (org.bson.Document) updates.getValue().getUpdateObject().get("$set");
        assertEquals(2, set.get("unreadCommentCount"));
        assertTrue(after.getComments().get(0).getUnread());
        assertFalse(after.getComments().get(1).getUnread());
        assertTrue(after.getComments().get(2).getUnread());
    }
}
