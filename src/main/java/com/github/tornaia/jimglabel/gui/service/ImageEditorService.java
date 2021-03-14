package com.github.tornaia.jimglabel.gui.service;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.common.setting.UserSettings;
import com.github.tornaia.jimglabel.common.setting.UserSettingsProvider;
import com.github.tornaia.jimglabel.gui.domain.Annotation;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
import com.github.tornaia.jimglabel.gui.domain.ObjectClass;
import com.github.tornaia.jimglabel.gui.domain.ObjectClasses;
import com.github.tornaia.jimglabel.gui.event.EditableImageEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ImageEditorService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageEditorService.class);

    private static final String CLASSES_FILENAME = "classes.json";

    private final UserSettingsProvider userSettingsProvider;
    private final EditableImageEventPublisher editableImageEventPublisher;
    private final SerializerUtils serializerUtils;

    private EditableImage editableImage;
    private int currentImageIndex;

    @Autowired
    public ImageEditorService(UserSettingsProvider userSettingsProvider, EditableImageEventPublisher editableImageEventPublisher, SerializerUtils serializerUtils) {
        this.userSettingsProvider = userSettingsProvider;
        this.editableImageEventPublisher = editableImageEventPublisher;
        this.serializerUtils = serializerUtils;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void applicationStarted() {
        loadImage();
    }

    public void updateSourceDirectory(String absolutePath) {
        userSettingsProvider.update(userSettings -> userSettings.setSourceDirectory(absolutePath));
        editableImageEventPublisher.updateSelectedImage(editableImage);
    }

    public void updateWorkspaceDirectory(String absolutePath) {
        userSettingsProvider.update(userSettings -> userSettings.setWorkspaceDirectory(absolutePath));
    }

    public void deleteCurrentImage() {
        try {
            Files.delete(editableImage.getFile());
            Files.deleteIfExists(getAnnotationFile());
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        loadImage();
        editableImageEventPublisher.updateSelectedImage(editableImage);
    }

    public void deleteDetectedObject(DetectedObject detectedObject) {
        editableImage.getDetectedObjects().remove(detectedObject);
        updateAnnotationFile();
        editableImageEventPublisher.updateDetectedObjects();
    }

    public void deleteAllObjects() {
        editableImage.getDetectedObjects().clear();
        updateAnnotationFile();
        editableImageEventPublisher.updateDetectedObjects();
    }

    public void updateDetectedObjectName(DetectedObject detectedObject, Integer newId) {
        detectedObject.setId(newId);
        updateAnnotationFile();
    }

    public void loadPreviousImage() {
        currentImageIndex--;
        loadImage();
    }

    public void loadNextImage() {
        currentImageIndex++;
        loadImage();
    }

    public String getSourceDirectory() {
        String sourceDirectory = userSettingsProvider.read().getSourceDirectory();
        if (sourceDirectory == null) {
            return null;
        }

        Path sourceDirectoryFile = Path.of(sourceDirectory);
        boolean sourceDirectoryExist = Files.isDirectory(sourceDirectoryFile);
        String sourceDirectoryAbsolutePath = sourceDirectoryFile.toAbsolutePath().toString();
        return sourceDirectoryExist ? sourceDirectoryAbsolutePath : null;
    }

    public String getWorkspaceDirectory() {
        String workspaceDirectory = userSettingsProvider.read().getWorkspaceDirectory();
        if (workspaceDirectory == null) {
            return null;
        } else {
            Path workspaceDirectoryFile = Path.of(workspaceDirectory);
            boolean workspaceDirectoryExist = Files.isDirectory(workspaceDirectoryFile);
            String workspaceDirectoryAbsolutePath = workspaceDirectoryFile.toAbsolutePath().toString();
            return workspaceDirectoryExist ? workspaceDirectoryAbsolutePath : null;
        }
    }

    public List<ObjectClass> getClasses() {
        UserSettings userSettings = userSettingsProvider.read();
        String sourceDirectory = userSettings.getSourceDirectory();
        Path classesJsonPath = Path.of(sourceDirectory).resolve(CLASSES_FILENAME);

        String classesFileContent;
        try {
            classesFileContent = Files.readString(classesJsonPath);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        ObjectClasses objectClasses = serializerUtils.toObject(classesFileContent, ObjectClasses.class);
        return objectClasses.getClasses();
    }

    public List<Path> getSourceImageFiles() {
        String sourceDirectory = getSourceDirectory();
        if (sourceDirectory == null) {
            return Collections.emptyList();
        }

        try {
            return Files.list(Path.of(sourceDirectory))
                    .flatMap(f -> {
                        try {
                            return Files.isDirectory(f) ? Files.list(f) : Stream.of(f);
                        } catch (IOException e) {
                            throw new IllegalStateException("Must not happen", e);
                        }
                    })
                    .filter(e -> e.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".jpg"))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
    }

    public void loadImage() {
        editableImage = null;

        List<Path> imageFiles = getSourceImageFiles();

        if (imageFiles.isEmpty()) {
            currentImageIndex = -1;
            return;
        }

        if (currentImageIndex < 0) {
            currentImageIndex = imageFiles.size() - 1;
        }

        if (currentImageIndex >= imageFiles.size()) {
            currentImageIndex = 0;
        }

        Path currentImage = imageFiles.get(currentImageIndex);
        byte[] content = getContent(currentImage);
        BufferedImage bufferedImage = getBufferedImage(currentImage);

        editableImage = new EditableImage(currentImage, bufferedImage, new ArrayList<>(), new ArrayList<>(), content);
        Annotation annotation = getAnnotation();
        editableImage.getDetectedObjects().addAll(annotation.getObjects());

        editableImageEventPublisher.updateSelectedImage(editableImage);
    }

    private Annotation getAnnotation() {
        Path annotationFile = getAnnotationFile();
        boolean hasAnnotationFile = Files.isRegularFile(annotationFile);
        if (hasAnnotationFile) {
            String annotationContent;
            try {
                annotationContent = Files.readString(annotationFile);
            } catch (IOException e) {
                throw new IllegalStateException("Must not happen", e);
            }

            return serializerUtils.toObject(annotationContent, Annotation.class);
        }

        String currentImageFileName = editableImage.getFile().getFileName().toString();
        long size = editableImage.getContent().length;
        BufferedImage bufferedImage = editableImage.getBufferedImage();
        return new Annotation(currentImageFileName, size, bufferedImage.getWidth(), bufferedImage.getHeight(), new ArrayList<>());
    }

    private Path getAnnotationFile() {
        Path currentImage = editableImage.getFile();
        String currentImageFileName = currentImage.getFileName().toString();
        String annotationFileName = currentImageFileName.substring(0, currentImageFileName.lastIndexOf('.')) + ".json";
        return currentImage.resolveSibling(annotationFileName);
    }

    private void updateAnnotationFile() {
        Path currentImage = editableImage.getFile();
        String currentImageFileName = currentImage.getFileName().toString();
        BufferedImage bufferedImage = editableImage.getBufferedImage();
        List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();
        long size = editableImage.getContent().length;

        Path annotationFile = getAnnotationFile();
        Annotation annotation = new Annotation(currentImageFileName, size, bufferedImage.getWidth(), bufferedImage.getHeight(), detectedObjects);
        String annotationFileContent = serializerUtils.toJSON(annotation);
        try {
            Files.writeString(annotationFile, annotationFileContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        Integer id = detectedObjects.size() > 0 ? detectedObjects.get(0).getId() : null;
        List<ObjectClass> classes = getClasses();
        ObjectClass objectClass = classes
                .stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
        String name = objectClass != null ? objectClass.getName() : null;
        LOG.info("Annotation file updated: {}, name: {} ({})", annotationFile, name, id);
    }

    private static BufferedImage getBufferedImage(Path imageFile) {
        try {
            byte[] content = getContent(imageFile);
            return ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
    }

    private static byte[] getContent(Path imageFile) {
        try {
            return Files.readAllBytes(imageFile);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
    }

    public void forEachImage(Consumer<EditableImage> optimizer) {
        int startImageImage = currentImageIndex;
        while (true) {
            loadNextImage();
            try {
                optimizer.accept(editableImage);
            } catch (Exception e) {
                LOG.error("Failed to generate optimized image", e);
                return;
            }
            if (startImageImage == currentImageIndex) {
                return;
            }
        }
    }
}
