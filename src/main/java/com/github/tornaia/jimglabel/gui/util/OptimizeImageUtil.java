package com.github.tornaia.jimglabel.gui.util;

import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class OptimizeImageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(OptimizeImageUtil.class);

    private static final int TARGET_WIDTH = 1024;
    private static final int TARGET_HEIGHT = 1024;
    private static final double TARGET_OBJECT_TO_IMAGE_AREA_RATIO = 0.1;

    private OptimizeImageUtil() {
    }

    public static ImageWithMeta optimize(ImageWithMeta input) {
        if (input.getObjects().size() != 1) {
            throw new IllegalStateException("Must not happen, at least one object expected");
        }

        ImageWithMeta step1 = cropToAlignWithTargetAspectRatio(input);
        ImageWithMeta step2 = cropToAlignWithExpectedObjectToImageRatio(step1);
        if (step2 == null) {
            throw new IllegalStateException("Must not happen");
        }
        return resizeToAlignWithExpectedTarget(step2);
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

            resultObjects.add(new DetectedObject(object.getId(), top, right, bottom, left));
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

        // leave only a smaller subset of the image to reach (or to be close) to the TARGET_OBJECT_TO_IMAGE_AREA_RATIO
        if (objectToImageAreaRatio < TARGET_OBJECT_TO_IMAGE_AREA_RATIO) {
            double cutEdge = 1 - Math.sqrt(objectToImageAreaRatio / TARGET_OBJECT_TO_IMAGE_AREA_RATIO);
            double objectCenterX = (imageWidth * (detectedObject.getLeft() + detectedObject.getRight()) / 2);
            double objectCenterY = (imageHeight * (detectedObject.getTop() + detectedObject.getBottom()) / 2);

            double cutLeft = (cutEdge * imageWidth * (objectCenterX / imageWidth));
            double cutRight = ((cutEdge) * imageWidth * (1 - objectCenterX / imageWidth));
            double cutHorizontal = cutLeft + cutRight;
            int cropWidth = (int) (imageWidth - cutHorizontal);

            double cutTop = (cutEdge * imageHeight * (objectCenterY / imageHeight));
            double cutBottom = ((cutEdge) * imageHeight * (1 - objectCenterY / imageHeight));
            double cutVertical = cutTop + cutBottom;
            int cropHeight = (int) (imageHeight - cutVertical);

            double croppedImageAspectRatio = (double) cropWidth / cropHeight;
            double targetImageAspectRatio = (double) TARGET_WIDTH / TARGET_HEIGHT;
            if (croppedImageAspectRatio != targetImageAspectRatio) {
                LOG.warn("Failed to keep aspect ratio, cropped: {}x{}, target: {}x{}", cropWidth, cropHeight, TARGET_WIDTH, TARGET_HEIGHT);
                return null;
            }
            BufferedImage resultImage = bufferedImage.getSubimage((int) cutLeft, (int) cutTop, cropWidth, cropHeight);

            List<DetectedObject> resultObjects = new ArrayList<>();
            for (DetectedObject object : objects) {
                int x0 = (int) Math.max(cutLeft, (int) (object.getLeft() * bufferedImage.getWidth()));
                int x1 = (int) Math.min(cutLeft + cropWidth, (int) (object.getRight() * bufferedImage.getWidth()));
                int y0 = (int) Math.max(cutTop, (int) (object.getTop() * bufferedImage.getHeight()));
                int y1 = (int) Math.min(cutTop + cropHeight, (int) (object.getBottom() * bufferedImage.getHeight()));

                float left = (float) ((x0 - cutLeft) / (float) resultImage.getWidth());
                float right = (float) ((x1 - cutLeft) / (float) resultImage.getWidth());
                float top = (float) ((y0 - cutTop) / (float) resultImage.getHeight());
                float bottom = (float) ((y1 - cutTop) / (float) resultImage.getHeight());

                resultObjects.add(new DetectedObject(object.getId(), top, right, bottom, left));
            }

            return new ImageWithMeta(resultImage, resultObjects);
        }

        return input;
    }

    // resize image: image has the target width and height
    private static ImageWithMeta resizeToAlignWithExpectedTarget(ImageWithMeta input) {
        BufferedImage inputBufferedImage = input.getImage();
        int inputWidth = inputBufferedImage.getWidth();
        int inputHeight = inputBufferedImage.getHeight();

        if (TARGET_WIDTH > inputWidth || TARGET_HEIGHT > inputHeight) {
            LOG.info("Image or object area is too small thus result is not optimal, resized image: {}x{}, target: {}x{}", inputWidth, inputHeight, TARGET_WIDTH, TARGET_HEIGHT);
            // this should be handled in the previous step and this should give an error
        }

        Image scaledImage = inputBufferedImage.getScaledInstance(TARGET_WIDTH, TARGET_HEIGHT, Image.SCALE_SMOOTH);

        BufferedImage resultImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, input.getImage().getType());

        Graphics2D g2d = resultImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return new ImageWithMeta(resultImage, input.getObjects());
    }
}
