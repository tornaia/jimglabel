package com.github.tornaia.jimglabel.common.setting;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Properties;

@Component
public class ApplicationSettings {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationSettings.class);

    private final String desktopClientName;
    private final String installerVersion;
    private final String buildVersion;
    private final Path userHomeDir;
    private final Path jarDir;
    private final Path settingsDir;
    private final Path logsDir;
    private final String runtime;
    private final String operatingSystem;
    private final String timeZone;

    @Autowired
    public ApplicationSettings(@Value("${desktop.client.name}") String desktopClientName, @Value("${installer.version}") String installerVersion) {
        this.desktopClientName = desktopClientName;
        this.installerVersion = installerVersion;
        this.buildVersion = getProperties().getProperty("Build-Version");
        this.userHomeDir = Paths.get(System.getProperty("user.home"));
        this.jarDir = getJarDirInternal();
        this.settingsDir = getSettingsDirInternal();
        this.logsDir = getLogsDirInternal();
        this.runtime = getRuntimeInternal();
        this.operatingSystem = getOperatingSystemInternal();
        this.timeZone = getTimeZoneInternal();
        LOG.info("DesktopClientName: {}", desktopClientName);
        LOG.info("InstallerVersion: {}", installerVersion);
        LOG.info("BuildVersion: {}", buildVersion);
        LOG.info("UserHomeDir: {}", userHomeDir);
        LOG.info("JarDir: {}", jarDir);
        LOG.info("SettingsDir: {}", settingsDir);
        LOG.info("LogsDir: {}", logsDir);
        LOG.info("Runtime: {}", runtime);
        LOG.info("Operating system: {}", operatingSystem);
        LOG.info("Time zone: {}", timeZone);
    }

    public String getDesktopClientName() {
        return desktopClientName;
    }

    public String getInstallerVersion() {
        return installerVersion;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public Path getUserHomeDir() {
        return userHomeDir;
    }

    public Path getJarDir() {
        return jarDir;
    }

    public Path getSettingsDir() {
        return settingsDir;
    }

    public Path getLogsDir() {
        return logsDir;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getTimeZone() {
        return timeZone;
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        try {
            properties.load(ApplicationSettings.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read properties", e);
        }
        return properties;
    }

    private String getRuntimeInternal() {
        return Runtime.version().toString();
    }

    private String getOperatingSystemInternal() {
        return SystemUtils.OS_ARCH + "/" + SystemUtils.OS_NAME + "/" + SystemUtils.OS_VERSION;
    }

    private String getTimeZoneInternal() {
        ZoneId zoneId = ZoneId.systemDefault();
        return zoneId.getId();
    }

    private Path getJarDirInternal() {
        return getAppDir().resolve("app");
    }

    private Path getSettingsDirInternal() {
        return getAppDir().resolve("settings");
    }

    private Path getLogsDirInternal() {
        return getAppDir().resolve("logs");
    }

    private Path getAppDir() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return getUserHomeDir()
                    .resolve("AppData")
                    .resolve("Local")
                    .resolve(desktopClientName);
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return getUserHomeDir()
                    .resolve("Library")
                    .resolve("Application Support")
                    .resolve(desktopClientName);
        } else {
            throw new IllegalStateException("Unknown OS: " + SystemUtils.OS_NAME);
        }
    }
}
