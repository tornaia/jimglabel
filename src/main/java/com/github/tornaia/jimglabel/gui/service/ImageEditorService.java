package com.github.tornaia.jimglabel.gui.service;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.common.setting.UserSettings;
import com.github.tornaia.jimglabel.common.setting.UserSettingsProvider;
import com.github.tornaia.jimglabel.gui.domain.Annotation;
import com.github.tornaia.jimglabel.gui.domain.ObjectClasses;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class ImageEditorService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageEditorService.class);

    private static final String CLASSES_FILENAME = "!object-classes.json";

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
        this.loadImage();
    }

    public void updateSourceDirectory(String absolutePath) {
        userSettingsProvider.update(userSettings -> userSettings.setSourceDirectory(absolutePath));
        editableImageEventPublisher.updateSelectedImage(editableImage);
    }

    public void updateTargetDirectory(String absolutePath) {
        userSettingsProvider.update(userSettings -> userSettings.setTargetDirectory(absolutePath));
    }

    public void deleteCurrentImage() {
        String sourceDirectory = userSettingsProvider.read().getSourceDirectory();
        try {
            Files.delete(Path.of(sourceDirectory).resolve(editableImage.getCurrentImageFileName()));
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

    public void updateDetectedObjectName(DetectedObject detectedObject, String newId) {
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

    public String getTargetDirectory() {
        String targetDirectory = userSettingsProvider.read().getTargetDirectory();
        if (targetDirectory == null) {
            return null;
        } else {
            Path targetDirectoryFile = Path.of(targetDirectory);
            boolean targetDirectoryExist = Files.isDirectory(targetDirectoryFile);
            String targetDirectoryAbsolutePath = targetDirectoryFile.toAbsolutePath().toString();
            return targetDirectoryExist ? targetDirectoryAbsolutePath : null;
        }
    }

    public List<ObjectClasses.Class> getClasses() {
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

    public List<String> getSourceImageFileNames() {
        String sourceDirectory = getSourceDirectory();
        if (sourceDirectory == null) {
            return Collections.emptyList();
        }

        try {
            return Files.list(Path.of(sourceDirectory))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(e -> e.toLowerCase(Locale.ENGLISH).endsWith(".jpg"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
    }

    public void loadImage() {
        this.editableImage = null;

        List<String> imageFileNames = getSourceImageFileNames();

        if (imageFileNames.isEmpty()) {
            this.currentImageIndex = -1;
            return;
        }

        if (currentImageIndex < 0) {
            currentImageIndex = imageFileNames.size() - 1;
        }

        if (currentImageIndex >= imageFileNames.size()) {
            currentImageIndex = 0;
        }

        String currentImageFileName = imageFileNames.get(currentImageIndex);
        Path currentImage = Path.of(getSourceDirectory()).resolve(currentImageFileName);
        byte[] content;
        try {
            content = Files.readAllBytes(currentImage);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        editableImage = new EditableImage(currentImageFileName, bufferedImage, new ArrayList<>(), content);
        Annotation annotation = getAnnotation(content, bufferedImage);
        editableImage.getDetectedObjects().addAll(annotation.getObjects());

        editableImageEventPublisher.updateSelectedImage(editableImage);
    }

    private Annotation getAnnotation(byte[] currentImageBytes, BufferedImage bufferedImage) {
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

        return new Annotation(editableImage.getCurrentImageFileName(), currentImageBytes.length, bufferedImage.getWidth(), bufferedImage.getHeight(), new ArrayList<>());
    }

    private Path getAnnotationFile() {
        String currentImageFileName = editableImage.getCurrentImageFileName();
        String directory = userSettingsProvider.read().getSourceDirectory();
        String annotationFileName = currentImageFileName.substring(0, currentImageFileName.lastIndexOf('.')) + ".json";
        return Path.of(directory).resolve(annotationFileName);
    }

    private void updateAnnotationFile() {
        String currentImageFileName = editableImage.getCurrentImageFileName();
        BufferedImage bufferedImage = editableImage.getBufferedImage();
        List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();
        byte[] content = editableImage.getContent();

        Path annotationFile = getAnnotationFile();
        Annotation annotation = new Annotation(currentImageFileName, content.length, bufferedImage.getWidth(), bufferedImage.getHeight(), detectedObjects);
        String annotationFileContent = serializerUtils.toJSON(annotation);
        try {
            Files.writeString(annotationFile, annotationFileContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        String objectId = detectedObjects.size() > 0 ? detectedObjects.get(0).getId() : null;
        List<ObjectClasses.Class> classes = getClasses();
        ObjectClasses.Class objectClass = classes
                .stream()
                .filter(e -> e.getId().equals(objectId))
                .findFirst()
                .orElse(null);
        String classId = objectClass != null ? objectClass.getId() : null;
        String className = objectClass != null ? objectClass.getName() : null;
        LOG.info("Annotation file updated: {}, objectName: {} ({})", annotationFile, className, classId);
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
