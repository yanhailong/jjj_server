package com.jjg.game.gm.config;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

/**
 * @author 11
 * @date 2025/10/27 19:46
 */
public class ExcludeServiceFilter implements TypeFilter {
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        String className = metadataReader.getClassMetadata().getClassName();
        // ChannelHttpService排除
        return className.equals("com.jjg.game.core.service.ChannelHttpService");
    }
}
