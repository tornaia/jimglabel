package com.github.tornaia.jimglabel.gui.domain;

import java.awt.image.BufferedImage;
import java.util.List;

public class EditableImage {

    private final String currentImageFileName;
    private final BufferedImage bufferedImage;
    private final List<DetectedObject> detectedObjects;
    private final byte[] content;

    public EditableImage(String currentImageFileName, BufferedImage bufferedImage, List<DetectedObject> detectedObjects, byte[] content) {
        this.currentImageFileName = currentImageFileName;
        this.bufferedImage = bufferedImage;
        this.detectedObjects = detectedObjects;
        this.content = content;
    }

    public String getCurrentImageFileName() {
        return currentImageFileName;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public byte[] getContent() {
        return content;
    }
}
