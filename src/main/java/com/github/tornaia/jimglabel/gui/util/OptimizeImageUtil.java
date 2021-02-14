package com.github.tornaia.jimglabel.gui.util;

import com.github.tornaia.jimglabel.gui.domain.DetectedObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class OptimizeImageUtil {

    private static final int TARGET_WIDTH = 600;
    private static final int TARGET_HEIGHT = 600;
    private static final double TARGET_OBJECT_TO_IMAGE_AREA_RATIO = 0.15;

    private OptimizeImageUtil() {
    }

    public static ImageWithMeta optimize(ImageWithMeta input) {
        if (input.getObjects().size() != 1) {
            throw new IllegalStateException("Must not happen, at least one object expected");
        }

        ImageWithMeta preCropped = cropToAlignWithTargetAspectRatio(input);
        ImageWithMeta cropped = cropToAlignWithExpectedObjectToImageRatio(preCropped);
        ImageWithMeta resized = resizeToAlignWithExpectedTarget(cropped);

        return resized;
    }

    // crop image: image has the target aspect ratio
    public static ImageWithMeta cropToAlignWithTargetAspectRatio(ImageWithMeta input) {
        BufferedImage bufferedImage = input.getImage();
        List<DetectedObject> objects = input.getObjects();

        if (objects.size() != 1) {
            throw new IllegalStateException("Must no happen, only 1 object per image is supported atm");
        }

        double targetAspectRatio = (double) TARGET_WIDTH / TARGET_HEIGHT;
        int imageWidth = bufferedImage.getWidth();
        int imageHeight = bufferedImage.getHeight();

        Dimension dimension = new Dimension(Math.min(imageWidth, (int) ((double) imageHeight * targetAspectRatio)), Math.min(imageHeight, (int) ((double) imageWidth * (1 / targetAspectRatio))));
        DetectedObject detectedObject = objects.get(0);
        double objectCenterX = (imageWidth * (detectedObject.getLeft() + detectedObject.getRight()) / 2);
        double objectCenterY = (imageHeight * (detectedObject.getTop() + detectedObject.getBottom()) / 2);
        int cropX0 = (int) (Math.max(0, objectCenterX - dimension.getWidth() / 2) + Math.min(0, imageWidth - objectCenterX - dimension.getWidth() / 2));
        int cropWidth = (int) dimension.getWidth();
        int cropY0 = (int) (Math.max(0, objectCenterY - dimension.getHeight() / 2) + Math.min(0, imageHeight - objectCenterY - dimension.getHeight() / 2));
        int cropHeight = (int) dimension.getHeight();

        BufferedImage resultImage = bufferedImage.getSubimage(cropX0, cropY0, cropWidth, cropHeight);

        List<DetectedObject> resultObjects = new ArrayList<>();
        for (DetectedObject object : objects) {
            int x0 = (int) (object.getLeft() * bufferedImage.getWidth());
            int x1 = (int) (object.getRight() * bufferedImage.getWidth());
            int y0 = (int) (object.getTop() * bufferedImage.getHeight());
            int y1 = (int) (object.getBottom() * bufferedImage.getHeight());
            if (x0 < cropX0 || x1 > cropX0 + cropWidth || y0 < cropY0 || y1 > cropY0 + cropHeight) {
                throw new IllegalStateException("Must not happen, object is outside of cropped area");
            }

            float left = (x0 - cropX0) / (float) resultImage.getWidth();
            float right = (x1 - cropX0) / (float) resultImage.getWidth();
            float top = (y0 - cropY0) / (float) resultImage.getHeight();
            float bottom = (y1 - cropY0) / (float) resultImage.getHeight();

            resultObjects.add(new DetectedObject(object.getName(), top, right, bottom, left));
        }

        return new ImageWithMeta(resultImage, resultObjects);
    }

    // crop image: object occupies 15% of the image
    public static ImageWithMeta cropToAlignWithExpectedObjectToImageRatio(ImageWithMeta input) {
        BufferedImage bufferedImage = input.getImage();
        List<DetectedObject> objects = input.getObjects();

        if (objects.size() != 1) {
            throw new IllegalStateException("Must no happen, only 1 object per image is supported atm");
        }

        int imageWidth = bufferedImage.getWidth();
        int imageHeight = bufferedImage.getHeight();

        long imageArea = (long) imageWidth * imageHeight;
        DetectedObject detectedObject = objects.get(0);
        int objectWidth = (int) ((detectedObject.getRight() - detectedObject.getLeft()) * imageWidth);
        int objectHeight = (int) ((detectedObject.getBottom() - detectedObject.getTop()) * imageHeight);
        long objectArea = (long) objectWidth * objectHeight;

        double objectToImageAreaRatio = (double) objectArea / imageArea;
        boolean cropRequired = objectToImageAreaRatio < TARGET_OBJECT_TO_IMAGE_AREA_RATIO;

        int cropX0;
        int cropWidth;
        int cropY0;
        int cropHeight;
        if (cropRequired) {
            double cutEdge = Math.sqrt(objectToImageAreaRatio / TARGET_OBJECT_TO_IMAGE_AREA_RATIO);
            double objectCenterX = (imageWidth * (detectedObject.getLeft() + detectedObject.getRight()) / 2);
            double objectCenterY = (imageHeight * (detectedObject.getTop() + detectedObject.getBottom()) / 2);
            double cutLeft = (1 - cutEdge) * objectCenterX / imageWidth;
            double cutTop = (1 - cutEdge) * objectCenterY / imageHeight;
            cropX0 = (int) (imageWidth * cutLeft);
            cropWidth = (int) (imageWidth * (1 - cutLeft));
            cropY0 = (int) (imageHeight * cutTop);
            cropHeight = (int) (imageHeight * (1 - cutTop));
        } else {
            cropX0 = 0;
            cropWidth = imageWidth;
            cropY0 = 0;
            cropHeight = imageHeight;
        }

        BufferedImage resultImage = bufferedImage.getSubimage(cropX0, cropY0, cropWidth, cropHeight);

        List<DetectedObject> resultObjects = new ArrayList<>();
        for (DetectedObject object : objects) {
            int x0 = (int) (object.getLeft() * bufferedImage.getWidth());
            int x1 = (int) (object.getRight() * bufferedImage.getWidth());
            int y0 = (int) (object.getTop() * bufferedImage.getHeight());
            int y1 = (int) (object.getBottom() * bufferedImage.getHeight());
            if (x0 < cropX0 || x1 > cropX0 + cropWidth || y0 < cropY0 || y1 > cropY0 + cropHeight) {
                throw new IllegalStateException("Must not happen, object is outside of cropped area");
            }

            float left = (x0 - cropX0) / (float) resultImage.getWidth();
            float right = (x1 - cropX0) / (float) resultImage.getWidth();
            float top = (y0 - cropY0) / (float) resultImage.getHeight();
            float bottom = (y1 - cropY0) / (float) resultImage.getHeight();

            resultObjects.add(new DetectedObject(object.getName(), top, right, bottom, left));
        }

        return new ImageWithMeta(resultImage, resultObjects);
    }

    // resize image: image has the target width and height
    private static ImageWithMeta resizeToAlignWithExpectedTarget(ImageWithMeta input) {
        Image image = input.getImage().getScaledInstance(TARGET_WIDTH, TARGET_HEIGHT, Image.SCALE_SMOOTH);

        BufferedImage resultImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, input.getImage().getType());

        Graphics2D g2d = resultImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return new ImageWithMeta(resultImage, input.getObjects());
    }
}
