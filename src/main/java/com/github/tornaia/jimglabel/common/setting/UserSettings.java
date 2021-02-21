package com.github.tornaia.jimglabel.common.setting;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class UserSettings extends AbstractSettings {

    private String sourceDirectory;
    private String workspaceDirectory;

    public UserSettings() {
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    public void setWorkspaceDirectory(String workspaceDirectory) {
        this.workspaceDirectory = workspaceDirectory;
    }

    @Override
    public String toString() {
        return new ToStringBuilder("UserSettings", ToStringStyle.JSON_STYLE)
                .append("UserSettings", "")
                .append("sourceDirectory", sourceDirectory)
                .append("workspaceDirectory", workspaceDirectory)
                .toString();
    }
}
