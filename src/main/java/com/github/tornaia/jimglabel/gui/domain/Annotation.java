package com.github.tornaia.jimglabel.gui.domain;

import java.util.List;

public class Annotation {

    private String name;
    private long size;
    private int width;
    private int height;
    private List<DetectedObject> objects;

    public Annotation() {
    }

    public Annotation(String name, long size, int width, int height, List<DetectedObject> objects) {
        this.name = name;
        this.size = size;
        this.width = width;
        this.height = height;
        this.objects = objects;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<DetectedObject> getObjects() {
        return objects;
    }

    public void setObjects(List<DetectedObject> objects) {
        this.objects = objects;
    }
}
