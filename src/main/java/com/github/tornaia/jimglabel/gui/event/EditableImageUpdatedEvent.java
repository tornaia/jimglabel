package com.github.tornaia.jimglabel.gui.event;

import com.github.tornaia.jimglabel.common.event.AbstractEvent;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;

public class EditableImageUpdatedEvent extends AbstractEvent {

    private final EditableImage editableImage;

    public EditableImageUpdatedEvent(EditableImage editableImage, long timestamp) {
        super(timestamp);
        this.editableImage = editableImage;
    }

    public EditableImage getEditableImage() {
        return editableImage;
    }
}
