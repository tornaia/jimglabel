package com.github.tornaia.jimglabel.gui.domain;

public class DetectedObject {

    private String id;
    private float top;
    private float right;
    private float bottom;
    private float left;

    public DetectedObject() {
    }

    public DetectedObject(String id, float top, float right, float bottom, float left) {
        if (top < 0 || right < 0 || bottom < 0 || left < 0 || top > 1 || right > 1 || bottom > 1 || left > 1) {
            throw new IllegalStateException("Must not happen");
        }
        this.id = id;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
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
