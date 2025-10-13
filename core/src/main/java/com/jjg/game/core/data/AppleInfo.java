package com.jjg.game.core.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/10/13 14:42
 */
@Component
@ConfigurationProperties(prefix = "thirdservice.apple")
public class AppleInfo {
}
