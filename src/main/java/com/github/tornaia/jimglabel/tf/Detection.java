package com.github.tornaia.jimglabel.tf;

public class Detection {

    private float top;
    private float left;
    private float bottom;
    private float right;
    private String label;
    private float score;

    public Detection() {
    }

    public Detection(float top, float left, float bottom, float right, String label, float score) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.label = label;
        this.score = score;
    }

    public float getTop() {
        return top;
    }

    public float getLeft() {
        return left;
    }

    public float getBottom() {
        return bottom;
    }

    public float getRight() {
        return right;
    }

    public String getLabel() {
        return label;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "Detection{" +
                "top=" + top +
                ", left=" + left +
                ", bottom=" + bottom +
                ", right=" + right +
                ", label='" + label + '\'' +
                ", score=" + score +
                '}';
    }
}
