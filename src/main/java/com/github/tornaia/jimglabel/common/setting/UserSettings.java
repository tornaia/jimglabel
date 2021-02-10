package com.github.tornaia.jimglabel.common.setting;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class UserSettings extends AbstractSettings {

    private String directory;

    public UserSettings() {
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public String toString() {
        return new ToStringBuilder("UserSettings", ToStringStyle.JSON_STYLE)
                .append("UserSettings", "")
                .append("directory", directory)
                .toString();
    }
}
