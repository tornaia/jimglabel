package com.github.tornaia.jimglabel.gui.service;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.gui.domain.Annotation;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
import com.github.tornaia.jimglabel.gui.util.ImageWithMeta;
import com.github.tornaia.jimglabel.gui.util.OptimizeImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.tornaia.jimglabel.gui.service.ImageEditorService.CLASSES_FILENAME;

@Component
public class OptimizeService {

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeService.class);

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

        String targetDirectory = imageEditorService.getTargetDirectory();
        if (targetDirectory == null) {
            // JOptionPane.showMessageDialog(jFrame, "Configure target directory");
            return;
        }

        Path sourceClassesFile = Path.of(sourceDirectory).resolve(CLASSES_FILENAME);
        Path targetClassesFile = Path.of(targetDirectory).resolve(CLASSES_FILENAME);
        try {
            Files.copy(sourceClassesFile, targetClassesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
        LOG.info("Classes file under target directory is updated: {}", targetClassesFile);

        Consumer<EditableImage> optimizer = e -> generateNextInternal(e, writeToDisk, Path.of(targetDirectory));
        imageEditorService.forEachImage(optimizer);
    }

    private void generateNextInternal(EditableImage editableImage, boolean writeToDisk, Path targetDirectoryFile) {
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
                .map(DetectedObject::getName)
                .anyMatch(Objects::isNull);
        if (missingName) {
            throw new IllegalStateException("Missing name for object");
        }

        String imageFileName = editableImage.getCurrentImageFileName();
        if (writeToDisk) {
            LOG.info("Generate optimized image of: {}", imageFileName);
        } else {
            LOG.info("Validate image: {}", imageFileName);
        }

        BufferedImage bufferedImage = editableImage.getBufferedImage();
        ImageWithMeta optimizedImage = OptimizeImageUtil.optimize(new ImageWithMeta(bufferedImage, detectedObjects));
        if (optimizedImage == null) {
            throw new IllegalStateException("Problematic image");
        }

        if (writeToDisk) {
            try {
                String optimizedImageFileName = imageFileName.substring(0, imageFileName.lastIndexOf('.')) + ".jpg";
                Path optimizedImagePath = targetDirectoryFile.resolve(optimizedImageFileName);
                ImageIO.write(optimizedImage.getImage(), "jpg", optimizedImagePath.toFile());
                long size = Files.size(optimizedImagePath);
                Path optimizedAnnotationPath = targetDirectoryFile.resolve(optimizedImageFileName.substring(0, optimizedImageFileName.lastIndexOf('.')) + ".json");
                Annotation annotation = new Annotation(optimizedImageFileName, size, optimizedImage.getImage().getWidth(), optimizedImage.getImage().getHeight(), optimizedImage.getObjects());
                String annotationContent = serializerUtils.toJSON(annotation);
                Files.writeString(optimizedAnnotationPath, annotationContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            } catch (IOException e) {
                throw new IllegalStateException("Must not happen", e);
            }
        }
    }
}
