package com.github.tornaia.jimglabel.gui.util;

import com.github.tornaia.jimglabel.gui.domain.DetectedObject;

import java.awt.*;

public final class DetectedObjectUtil {

    public static final int CONTROL_SIZE_RADIUS = 5;

    private static final int CONTROL_SIZE_RADIUS2 = CONTROL_SIZE_RADIUS + 5;

    private DetectedObjectUtil() {
    }

    public static boolean isSelected(Image scaledImage, DetectedObject object, Point point) {
        return isTopLeftControl(scaledImage, object, point) ||
                isTopControl(scaledImage, object, point) ||
                isTopRightControl(scaledImage, object, point) ||
                isLeftControl(scaledImage, object, point) ||
                isMoveControl(scaledImage, object, point) ||
                isRightControl(scaledImage, object, point) ||
                isBottomLeftControl(scaledImage, object, point) ||
                isBottomControl(scaledImage, object, point) ||
                isBottomRightControl(scaledImage, object, point);
    }

    public static boolean isTopLeftControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - x0, py - y0) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isTopControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - ((x0 + x1) / 2), py - y0) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isTopRightControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - x1, py - y0) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isLeftControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - x0, py - ((y0 + y1) / 2)) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isMoveControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - ((x0 + x1) / 2), py - ((y0 + y1) / 2)) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isRightControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - x1, py - ((y0 + y1) / 2)) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isBottomLeftControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - x0, py - y1) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isBottomControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - ((x0 + x1) / 2), py - y1) < CONTROL_SIZE_RADIUS2;
    }

    public static boolean isBottomRightControl(Image scaledImage, DetectedObject object, Point point) {
        double px = point.x;
        double py = point.y;

        float top = object.getTop();
        float right = object.getRight();
        float bottom = object.getBottom();
        float left = object.getLeft();
        float x0 = scaledImage.getWidth(null) * left;
        float y0 = scaledImage.getHeight(null) * top;
        float x1 = scaledImage.getWidth(null) * right;
        float y1 = scaledImage.getHeight(null) * bottom;

        return Math.hypot(px - x1, py - y1) < CONTROL_SIZE_RADIUS2;
    }

    public static ObjectControl getSelectedObjectControl(Image scaledImage, DetectedObject object, Point point) {
        if (object == null) {
            return null;
        }

        if (isTopLeftControl(scaledImage, object, point)) {
            return ObjectControl.TOP_LEFT;
        }

        if (isTopControl(scaledImage, object, point)) {
            return ObjectControl.TOP;
        }

        if (isTopRightControl(scaledImage, object, point)) {
            return ObjectControl.TOP_RIGHT;
        }

        if (isLeftControl(scaledImage, object, point)) {
            return ObjectControl.LEFT;
        }

        if (isMoveControl(scaledImage, object, point)) {
            return ObjectControl.MOVE;
        }

        if (isRightControl(scaledImage, object, point)) {
            return ObjectControl.RIGHT;
        }

        if (isBottomLeftControl(scaledImage, object, point)) {
            return ObjectControl.BOTTOM_LEFT;
        }

        if (isBottomControl(scaledImage, object, point)) {
            return ObjectControl.BOTTOM;
        }

        if (isBottomRightControl(scaledImage, object, point)) {
            return ObjectControl.BOTTOM_RIGHT;
        }

        return null;
    }
}
