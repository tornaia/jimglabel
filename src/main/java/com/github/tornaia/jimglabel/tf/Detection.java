package com.github.tornaia.jimglabel.tf;

public class Detection {

    private float top;
    private float left;
    private float bottom;
    private float right;
    private int id;
    private String cardId;
    private String name;
    private float score;

    public Detection() {
    }

    public Detection(float top, float left, float bottom, float right, int id, String cardId, String name, float score) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.id = id;
        this.cardId = cardId;
        this.name = name;
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

    public int getId() {
        return id;
    }

    public String getCardId() {
        return cardId;
    }

    public String getName() {
        return name;
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
                ", id='" + id + '\'' +
                ", cardId='" + cardId + '\'' +
                ", name='" + name + '\'' +
                ", score=" + score +
                '}';
    }
}
