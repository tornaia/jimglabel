package com.github.tornaia.jimglabel.gui.domain;

public class DetectedObject {

    private static final double TOLERANCE = 0.01D;

    private String id;
    private float top;
    private float right;
    private float bottom;
    private float left;

    public DetectedObject() {
    }

    public DetectedObject(String id, float top, float right, float bottom, float left) {
        if (top < 0D - TOLERANCE) {
            throw new IllegalStateException("Must not happen, top: " + top);
        } else if (right < 0D - TOLERANCE) {
            throw new IllegalStateException("Must not happen, right: " + right);
        } else if (bottom < 0D - TOLERANCE) {
            throw new IllegalStateException("Must not happen, bottom: " + bottom);
        } else if (left < 0D - TOLERANCE) {
            throw new IllegalStateException("Must not happen, left: " + left);
        } else if (top > 1D + TOLERANCE) {
            throw new IllegalStateException("Must not happen, top: " + top);
        } else if (right > 1D + TOLERANCE) {
            throw new IllegalStateException("Must not happen, right: " + right);
        } else if (bottom > 1D + TOLERANCE) {
            throw new IllegalStateException("Must not happen, bottom: " + bottom);
        } else if (left > 1D + TOLERANCE) {
            throw new IllegalStateException("Must not happen, left: " + left);
        } else if (top >= bottom) {
            throw new IllegalStateException("Must not happen, top: " + top + ", bottom: " + bottom);
        } else if (left >= right) {
            throw new IllegalStateException("Must not happen, left: " + left + ", right: " + right);
        }

        this.id = id;
        this.top = Math.max(0, Math.min(1, top));
        this.right = Math.max(0, Math.min(1, right));
        this.bottom = Math.max(0, Math.min(1, bottom));
        this.left = Math.max(0, Math.min(1, left));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }
}
