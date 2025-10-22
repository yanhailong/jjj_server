package com.jjg.game.account.config;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

/**
 * @author 11
 * @date 2025/10/18 9:52
 */
public class ExcludeManagerFilter implements TypeFilter {
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        String className = metadataReader.getClassMetadata().getClassName();
        // 如果类在 manager 包下，且不是 SampleDataManager，则排除
        return className.startsWith("com.jjg.game.core.manager")
                && !className.equals("com.jjg.game.core.manager.SampleDataManager")
                && !className.equals("com.jjg.game.core.manager.CoreSendMessageManager")
                && !className.equals("com.jjg.game.core.manager.ConfigManager")
                ;
    }
}
