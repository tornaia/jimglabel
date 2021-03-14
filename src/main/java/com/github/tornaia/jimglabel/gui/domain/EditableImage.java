package com.github.tornaia.jimglabel.gui.domain;

import com.github.tornaia.jimglabel.tf.Detection;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

public class EditableImage {

    private final Path file;
    private final BufferedImage bufferedImage;
    private final List<DetectedObject> detectedObjects;
    private final List<Detection> tensorFlowDetections;
    private final byte[] content;

    public EditableImage(Path file, BufferedImage bufferedImage, List<DetectedObject> detectedObjects, List<Detection> tensorFlowDetections, byte[] content) {
        this.file = file;
        this.bufferedImage = bufferedImage;
        this.detectedObjects = detectedObjects;
        this.tensorFlowDetections = tensorFlowDetections;
        this.content = content;
    }

    public Path getFile() {
        return file;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public List<Detection> getTensorFlowDetections() {
        return tensorFlowDetections;
    }

    public byte[] getContent() {
        return content;
    }
}
