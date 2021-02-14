package com.github.tornaia.jimglabel.gui.util;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.common.setting.UserSettings;
import com.github.tornaia.jimglabel.common.setting.UserSettingsProvider;
import com.github.tornaia.jimglabel.gui.domain.ClassesFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
public final class ClassUtil {

    private final UserSettingsProvider userSettingsProvider;
    private final SerializerUtils serializerUtils;

    @Autowired
    public ClassUtil(UserSettingsProvider userSettingsProvider, SerializerUtils serializerUtils) {
        this.userSettingsProvider = userSettingsProvider;
        this.serializerUtils = serializerUtils;
    }

    public List<String> getClasses() {
        UserSettings userSettings = userSettingsProvider.read();
        String sourceDirectory = userSettings.getSourceDirectory();
        Path classesJsonPath = Path.of(sourceDirectory).resolve("!classes.json");

        String classesFileContent;
        try {
            classesFileContent = Files.readString(classesJsonPath);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        ClassesFile classesFile = serializerUtils.toObject(classesFileContent, ClassesFile.class);
        return classesFile
                .getClasses()
                .stream()
                .map(ClassesFile.Class::getName)
                .collect(Collectors.toList());
    }
}
