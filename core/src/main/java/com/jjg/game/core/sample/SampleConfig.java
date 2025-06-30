package com.jjg.game.core.sample;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author 11
 * @date 2025/6/6 17:46
 */
@Configuration
@ConfigurationProperties(prefix = "sample")
public class SampleConfig {
    public String samplePackage = "com.jjg.game.sample";

    public String samplePath = "resources/sample/dollarexpress";

    public String versionFile;

    public String getSamplePackage() {
        return samplePackage;
    }

    public void setSamplePackage(String samplePackage) {
        this.samplePackage = samplePackage;
    }

    public String getSamplePath() {
        return samplePath;
    }

    public void setSamplePath(String samplePath) {
        this.samplePath = samplePath;
    }

    public String getVersionFile() {
        return versionFile;
    }

    public void setVersionFile(String versionFile) {
        this.versionFile = versionFile;
    }
}
