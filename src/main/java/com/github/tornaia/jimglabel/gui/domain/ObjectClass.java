package com.github.tornaia.jimglabel.gui.domain;

public final class ObjectClass {

    private Integer id;
    private String cardId;
    private String name;

    public ObjectClass() {
    }

    public ObjectClass(int id, String cardId, String name) {
        this.id = id;
        this.cardId = cardId;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}