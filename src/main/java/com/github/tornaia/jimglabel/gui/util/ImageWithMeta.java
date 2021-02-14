package com.github.tornaia.jimglabel.gui.util;

import com.github.tornaia.jimglabel.gui.domain.DetectedObject;

import java.awt.image.BufferedImage;
import java.util.List;

public class ImageWithMeta {

    private final BufferedImage image;
    private final List<DetectedObject> objects;

    public ImageWithMeta(BufferedImage image, List<DetectedObject> objects) {
        this.image = image;
        this.objects = objects;
    }

    public BufferedImage getImage() {
        return image;
    }

    public List<DetectedObject> getObjects() {
        return objects;
    }
}
