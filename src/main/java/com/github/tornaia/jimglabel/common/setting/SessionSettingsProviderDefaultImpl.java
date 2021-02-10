package com.github.tornaia.jimglabel.common.setting;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SessionSettingsProviderDefaultImpl implements SessionSettingsProvider {

    private final ApplicationSettings applicationSettings;

    @Autowired
    public SessionSettingsProviderDefaultImpl(ApplicationSettings applicationSettings) {
        this.applicationSettings = applicationSettings;
    }

    @Override
    public Path getUserDirectory() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return Paths.get(System.getenv("USERPROFILE"));
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return Paths.get(System.getenv("HOME"));
        } else {
            throw new IllegalStateException("Unknown OS: " + SystemUtils.OS_NAME);
        }
    }

    @Override
    public Path getLogsDirectory() {
        return getAppRootDirectory().resolve("logs");
    }

    @Override
    public Path getSettingsDirectory() {
        return getAppRootDirectory().resolve("settings");
    }

    private Path getAppRootDirectory() {
        Path userDirectory = getUserDirectory();

        if (SystemUtils.IS_OS_WINDOWS) {
            return userDirectory
                    .resolve("AppData")
                    .resolve("Local")
                    .resolve(applicationSettings.getDesktopClientName());
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return userDirectory
                    .resolve("Library")
                    .resolve("Application Support")
                    .resolve(applicationSettings.getDesktopClientName());
        } else {
            throw new IllegalStateException("Unknown OS: " + SystemUtils.OS_NAME);
        }
    }
}
