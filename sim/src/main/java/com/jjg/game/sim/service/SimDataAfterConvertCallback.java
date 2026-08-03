package com.jjg.game.sim.service;

import com.jjg.game.sim.data.AbstractData;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.stereotype.Component;

/** 初始化数据库加载数据的保存基线，避免未修改文档在首次自动保存时被全量重写。 */
@Component
public class SimDataAfterConvertCallback implements AfterConvertCallback<AbstractData> {

    @Override
    public AbstractData onAfterConvert(AbstractData entity, Document document, String collection) {
        entity.markSaved(SimAutoSaveService.snapshotHash(document));
        return entity;
    }
}
