package com.github.tornaia.jimglabel.gui.domain;

public class DetectedObject {

    private String name;
    private float top;
    private float right;
    private float bottom;
    private float left;

    public DetectedObject() {
    }

    public DetectedObject(String name, float top, float right, float bottom, float left) {
        if (top < 0 || right < 0 || bottom < 0 || left < 0 || top > 1 || right > 1 || bottom > 1 || left > 1) {
            throw new IllegalStateException("Must not happen");
        }
        this.name = name;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
