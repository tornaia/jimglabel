package com.github.tornaia.jimglabel.common.setting;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class UserSettings extends AbstractSettings {

    private String sourceDirectory;
    private String targetDirectory;

    public UserSettings() {
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @Override
    public String toString() {
        return new ToStringBuilder("UserSettings", ToStringStyle.JSON_STYLE)
                .append("UserSettings", "")
                .append("sourceDirectory", sourceDirectory)
                .append("targetDirectory", targetDirectory)
                .toString();
    }
}
