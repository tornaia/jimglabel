package com.github.tornaia.jimglabel.gui.util;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.gui.domain.ClassesFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class ClassUtil {

    private static final SerializerUtils SERIALIZER_UTILS = new SerializerUtils();

    public static List<String> getClasses() {
        String classesFileContent;
        try {
            classesFileContent = Files.readString(Path.of("C:/temp/!tejes/!classes.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        ClassesFile classesFile = SERIALIZER_UTILS.toObject(classesFileContent, ClassesFile.class);
        return classesFile
                .getClasses()
                .stream()
                .map(ClassesFile.Class::getName)
                .collect(Collectors.toList());
    }

}
