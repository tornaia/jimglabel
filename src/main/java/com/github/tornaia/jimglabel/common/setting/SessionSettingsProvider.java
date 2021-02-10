package com.github.tornaia.jimglabel.common.setting;

import java.nio.file.Path;

public interface SessionSettingsProvider {

    Path getUserDirectory();

    Path getLogsDirectory();

    Path getSettingsDirectory();
}
