package com.github.tornaia.jimglabel.gui.service;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.gui.domain.Annotation;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
import com.github.tornaia.jimglabel.gui.domain.ObjectClasses;
import com.github.tornaia.jimglabel.gui.util.ImageWithMeta;
import com.github.tornaia.jimglabel.gui.util.OptimizeImageUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static com.github.tornaia.jimglabel.gui.util.StringUtils.normalize;

@Component
public class OptimizeService {

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeService.class);

    private static final String ANNOTATIONS_DIRECTORY = "annotations";
    private static final String IMAGES_DIRECTORY = "images";
    private static final String TRAIN_DIRECTORY = "train";
    private static final String TEST_DIRECTORY = "test";
    private static final String LABEL_MAP_FILENAME = "label_map.pbtxt";

    private final ImageEditorService imageEditorService;
    private final SerializerUtils serializerUtils;

    @Autowired
    public OptimizeService(ImageEditorService imageEditorService, SerializerUtils serializerUtils) {
        this.imageEditorService = imageEditorService;
        this.serializerUtils = serializerUtils;
    }

    public void generateImages(boolean writeToDisk) {
        String sourceDirectory = imageEditorService.getSourceDirectory();
        if (sourceDirectory == null) {
            // JOptionPane.showMessageDialog(jFrame, "Configure source directory");
            return;
        }

        String workspaceDirectory = imageEditorService.getWorkspaceDirectory();
        if (workspaceDirectory == null) {
            // JOptionPane.showMessageDialog(jFrame, "Configure target directory");
            return;
        }

        try {
            Files.createDirectories(Path.of(workspaceDirectory).resolve(ANNOTATIONS_DIRECTORY));
            FileUtils.cleanDirectory(Path.of(workspaceDirectory).resolve(ANNOTATIONS_DIRECTORY).toFile());
            Files.createDirectories(Path.of(workspaceDirectory).resolve(IMAGES_DIRECTORY));
            FileUtils.cleanDirectory(Path.of(workspaceDirectory).resolve(IMAGES_DIRECTORY).toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        List<ObjectClasses.Class> classes = imageEditorService.getClasses();

        Map<String, Set<String>> generatedImagesPerClasses = new HashMap<>();
        Consumer<EditableImage> optimizer = e -> generateNextInternal(e, writeToDisk, Path.of(workspaceDirectory), classes, generatedImagesPerClasses);
        imageEditorService.forEachImage(optimizer);

        if (writeToDisk) {
            splitGeneratedImages(Path.of(workspaceDirectory), generatedImagesPerClasses, 0.2D, classes);
        }
        generateLabelMap(Path.of(workspaceDirectory), classes, writeToDisk);
    }

    private void generateNextInternal(EditableImage editableImage, boolean writeToDisk, Path workspaceDirectoryFile, List<ObjectClasses.Class> classes, Map<String, Set<String>> generatedImagesPerClasses) {
        List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();

        boolean noObject = detectedObjects.isEmpty();
        if (noObject) {
            throw new IllegalStateException("No object found on this image");
        }

        boolean multipleObjects = detectedObjects.size() > 1;
        if (multipleObjects) {
            throw new IllegalStateException("Multiple objects found on this image");
        }

        boolean missingName = detectedObjects
                .stream()
                .map(DetectedObject::getId)
                .anyMatch(Objects::isNull);
        if (missingName) {
            throw new IllegalStateException("No object name defined");
        }

        String imageFileName = editableImage.getFile().getFileName().toString();
        LOG.info("Generate optimized image of: {}", imageFileName);

        BufferedImage bufferedImage = editableImage.getBufferedImage();
        ImageWithMeta optimizedImage = OptimizeImageUtil.optimize(new ImageWithMeta(bufferedImage, detectedObjects));

        DetectedObject detectedObject = optimizedImage.getObjects().get(0);
        ObjectClasses.Class detectedObjectClass = classes
                .stream()
                .filter(e -> e.getId().equals(detectedObject.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to find class for id: " + detectedObject.getId()));

        Set<String> images = generatedImagesPerClasses.computeIfAbsent(detectedObjectClass.getId(), (k) -> new HashSet<>());
        String optimizedImageFileName = String.format("%s_%s_%06d.jpg", detectedObjectClass.getId(), normalize(detectedObjectClass.getName()), images.size());
        images.add(optimizedImageFileName);
        ByteArrayOutputStream optimizedImageContentOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(optimizedImage.getImage(), "jpg", optimizedImageContentOutputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
        byte[] optimizedImageContent = optimizedImageContentOutputStream.toByteArray();
        long optimizedImageLength = optimizedImageContent.length;
        Annotation annotation = new Annotation(optimizedImageFileName, optimizedImageLength, optimizedImage.getImage().getWidth(), optimizedImage.getImage().getHeight(), optimizedImage.getObjects());
        String annotationContent = serializerUtils.toJSON(annotation);

        if (writeToDisk) {
            try {
                Path optimizedImagePath = workspaceDirectoryFile.resolve(IMAGES_DIRECTORY).resolve(optimizedImageFileName);
                Path optimizedAnnotationPath = workspaceDirectoryFile.resolve(IMAGES_DIRECTORY).resolve(getJsonFileName(optimizedImageFileName));
                Files.write(optimizedImagePath, optimizedImageContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
                Files.writeString(optimizedAnnotationPath, annotationContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            } catch (IOException e) {
                throw new IllegalStateException("Must not happen", e);
            }
        }
    }

    private void splitGeneratedImages(Path workspaceDirectory, Map<String, Set<String>> generatedImagesPerClasses, double testRatio, List<ObjectClasses.Class> classes) {
        Path trainDirectory = workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(TRAIN_DIRECTORY);
        Path testDirectory = workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(TEST_DIRECTORY);

        try {
            Files.createDirectory(trainDirectory);
            Files.createDirectory(testDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        for (Map.Entry<String, Set<String>> generatedImagesPerClass : generatedImagesPerClasses.entrySet()) {
            String classId = generatedImagesPerClass.getKey();
            List<String> images = new ArrayList<>(generatedImagesPerClass.getValue());
            int imageCount = images.size();
            int testCount = (int) (testRatio * imageCount);
            if (testCount < 1) {
                throw new IllegalStateException("At least one test image required but got: " + testCount);
            }

            Collections.shuffle(images);

            for (int i = 0; i < testCount; i++) {
                String testImageFileName = images.remove(0);
                try {
                    Files.move(workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(testImageFileName), testDirectory.resolve(testImageFileName));
                    Annotation annotation = serializerUtils.toObject(Files.readString(workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(getJsonFileName(testImageFileName))), Annotation.class);
                    Path annotationFile = workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(getJsonFileName(testImageFileName));
                    Files.writeString(testDirectory.resolve(getXmlFileName(testImageFileName)), createXmlContent(workspaceDirectory, "test", annotation, classes));
                    Files.delete(annotationFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Must not happen", e);
                }
            }

            images.forEach(trainImageFileName -> {
                try {
                    Files.move(workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(trainImageFileName), trainDirectory.resolve(trainImageFileName));
                    Annotation annotation = serializerUtils.toObject(Files.readString(workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(getJsonFileName(trainImageFileName))), Annotation.class);
                    Path annotationFile = workspaceDirectory.resolve(IMAGES_DIRECTORY).resolve(getJsonFileName(trainImageFileName));
                    Files.writeString(trainDirectory.resolve(getXmlFileName(trainImageFileName)), createXmlContent(workspaceDirectory, "train", annotation, classes));
                    Files.delete(annotationFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Must not happen", e);
                }
            });

            String className = classes.stream()
                    .filter(e -> e.getId().equals(classId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Failed to find class for id: " + classId))
                    .getName();
            LOG.info("{} ({}) has {} train and {} test images", className, classId, imageCount - testCount, testCount);
        }
    }

    private String getJsonFileName(String imageFileName) {
        return imageFileName.substring(0, imageFileName.lastIndexOf('.')) + ".json";
    }

    private String getXmlFileName(String imageFileName) {
        return imageFileName.substring(0, imageFileName.lastIndexOf('.')) + ".xml";
    }

    private String createXmlContent(Path workspaceDirectory, String type, Annotation annotation, List<ObjectClasses.Class> classes) {
        String filename = annotation.getName();
        String path = workspaceDirectory.resolve(type).resolve(filename).toAbsolutePath().toString();
        int width = annotation.getWidth();
        int height = annotation.getHeight();
        List<DetectedObject> objects = annotation.getObjects();
        DetectedObject object = objects.get(0);
        ObjectClasses.Class objectClass = classes.stream()
                .filter(e -> e.getId().equals(object.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to find class for id: " + object.getId()));

        String objectName = objectClass.getId();
        int xmin = Math.round(object.getLeft() * width);
        int ymin = Math.round(object.getTop() * height);
        int xmax = Math.round(object.getRight() * width);
        int ymax = Math.round(object.getBottom() * height);

        return "<annotation>" + System.lineSeparator() +
                "\t<folder>" + type + "</folder>" + System.lineSeparator() + //
                "\t<filename>" + filename + "</filename>" + System.lineSeparator() + // 20210131_150532.jpg
                "\t<path>" + path + "</path>" + System.lineSeparator() + // C:\workspace\tensorflow2\workspace\training_demo\images\test\20210131_150532.jpg
                "\t<source>" + System.lineSeparator() +
                "\t\t<database>Unknown</database>" + System.lineSeparator() +
                "\t</source>" + System.lineSeparator() +
                "\t<size>" + System.lineSeparator() +
                "\t\t<width>" + width + "</width>" + System.lineSeparator() + // 4032
                "\t\t<height>" + height + "</height>" + System.lineSeparator() + // 2268
                "\t\t<depth>3</depth>" + System.lineSeparator() +
                "\t</size>" + System.lineSeparator() +
                "\t<segmented>0</segmented>" + System.lineSeparator() +
                "\t<object>" + System.lineSeparator() +
                "\t\t<name>" + objectName + "</name>" + System.lineSeparator() + // 0556-Orwella-papno
                "\t\t<pose>Unspecified</pose>" + System.lineSeparator() +
                "\t\t<truncated>0</truncated>" + System.lineSeparator() +
                "\t\t<difficult>0</difficult>" + System.lineSeparator() +
                "\t\t<bndbox>" + System.lineSeparator() +
                "\t\t\t<xmin>" + xmin + "</xmin>" + System.lineSeparator() + // 1447
                "\t\t\t<ymin>" + ymin + "</ymin>" + System.lineSeparator() + // 758
                "\t\t\t<xmax>" + xmax + "</xmax>" + System.lineSeparator() + // 2486
                "\t\t\t<ymax>" + ymax + "</ymax>" + System.lineSeparator() + // 1686
                "\t\t</bndbox>" + System.lineSeparator() +
                "\t</object>" + System.lineSeparator() +
                "</annotation>" + System.lineSeparator();
    }

    private void generateLabelMap(Path workspaceDirectory, List<ObjectClasses.Class> classes, boolean writeToDisk) {
        Path labelMapPath = workspaceDirectory.resolve(ANNOTATIONS_DIRECTORY).resolve(LABEL_MAP_FILENAME);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < classes.size(); i++) {
            ObjectClasses.Class objectClass = classes.get(i);
            String entry =
                    "item {" + System.lineSeparator() +
                            "  id: " + (i + 1) + System.lineSeparator() +
                            "  name: '" + objectClass.getId() + "'" + System.lineSeparator() +
                            "}" + System.lineSeparator() +
                            "" + System.lineSeparator();
            sb.append(entry);
        }
        String labelMapContent = sb.toString().trim();

        if (writeToDisk) {
            try {
                Files.writeString(labelMapPath, labelMapContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            } catch (IOException e) {
                throw new IllegalStateException("Must not happen", e);
            }
        }
    }
}
