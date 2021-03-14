package com.github.tornaia.jimglabel.gui.component;

import java.util.Objects;

public class AutoCompleteItem {

    private final int id;
    private final String label;

    public AutoCompleteItem(int id, String label) {
        if (label == null) {
            throw new IllegalStateException("Must not happen, label must not be null");
        }

        this.id = id;
        this.label = label;
    }

    public int getId() {
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
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
