package com.github.tornaia.jimglabel.gui.domain;

import java.util.Objects;

public class AutoCompleteItem {

    private final String id;
    private final String label;

    public AutoCompleteItem(String id, String label) {
        if (id == null) {
            throw new IllegalStateException("Must not happen, id must not be null");
        }
        if (label == null) {
            throw new IllegalStateException("Must not happen, label must not be null");
        }

        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoCompleteItem that = (AutoCompleteItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
