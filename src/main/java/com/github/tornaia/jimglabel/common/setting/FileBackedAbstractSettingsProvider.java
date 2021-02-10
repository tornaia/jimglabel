package com.github.tornaia.jimglabel.common.setting;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public abstract class FileBackedAbstractSettingsProvider<T extends AbstractSettings> {

    private static final Logger LOG = LoggerFactory.getLogger(FileBackedAbstractSettingsProvider.class);

    private final String name;
    private final SerializerUtils serializerUtils;
    private final SessionSettingsProvider sessionSettingsProvider;

    private final T settings;

    private T unmodifiableSettings;

    public FileBackedAbstractSettingsProvider(String name, Class<T> settingsClass, SessionSettingsProvider sessionSettingsProvider, SerializerUtils serializerUtils) {
        this.name = name;
        this.serializerUtils = serializerUtils;
        this.sessionSettingsProvider = sessionSettingsProvider;
        this.settings = readSettingsFile(settingsClass);
        this.unmodifiableSettings = settings.copy();
        LOG.info("Initial content of settings file, name: {}, content: {}", name, unmodifiableSettings);
    }

    public final synchronized void update(Consumer<T> settingsConsumer) {
        settingsConsumer.accept(settings);

        String settingsJson = serializerUtils.toJSON(settings);

        Path logsDirectory = sessionSettingsProvider.getSettingsDirectory();
        Path settingsJsonFile = logsDirectory.resolve(name + ".settings.json");

        byte[] contentAsBytes = settingsJson.getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = Files.newOutputStream(settingsJsonFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            outputStream.write(contentAsBytes, 0, contentAsBytes.length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write to settings json file", e);
        }

        unmodifiableSettings = settings.copy();
    }

    public final synchronized T read() {
        return unmodifiableSettings;
    }

    private T readSettingsFile(Class<T> clazz) {
        Path settingsDirectory = sessionSettingsProvider.getSettingsDirectory();
        try {
            Files.createDirectories(settingsDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create settings json directory", e);
        }

        Path settingsJsonFile = settingsDirectory.resolve(name + ".settings.json");

        boolean settingsJsonExists = Files.exists(settingsJsonFile);
        if (!settingsJsonExists) {
            LOG.info("Settings json file does not exist, creating a new one");
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Failed to create settings object", e);
            }
        }

        byte[] settingsJsonBytes;
        try {
            settingsJsonBytes = Files.readAllBytes(settingsJsonFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read settings json file", e);
        }

        return serializerUtils.toObject(new String(settingsJsonBytes), clazz);
    }
}
