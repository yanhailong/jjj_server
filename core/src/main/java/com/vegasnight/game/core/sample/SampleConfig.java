package com.vegasnight.game.core.sample;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author 11
 * @date 2025/6/6 17:46
 */
@Configuration
@ConfigurationProperties(prefix = "sample")
public class SampleConfig {
    public String samplePackage = "com.vegasnight.game.sample";

    public String samplePath = "resources";

    public String i18nFile;

    public int roleSaveInterval = 60;

    public boolean auto;

    public String versionFile;

    public String updateFolder;

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

    public String getI18nFile() {
        return i18nFile;
    }

    public void setI18nFile(String i18nFile) {
        this.i18nFile = i18nFile;
    }

    public int getRoleSaveInterval() {
        return roleSaveInterval;
    }

    public void setRoleSaveInterval(int roleSaveInterval) {
        this.roleSaveInterval = roleSaveInterval;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public String getVersionFile() {
        return versionFile;
    }

    public void setVersionFile(String versionFile) {
        this.versionFile = versionFile;
    }

    public String getUpdateFolder() {
        return updateFolder;
    }

    public void setUpdateFolder(String updateFolder) {
        this.updateFolder = updateFolder;
    }
}
