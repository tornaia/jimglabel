package com.github.tornaia.jimglabel.gui.event;

import com.github.tornaia.jimglabel.common.event.AbstractEvent;

public class DetectedObjectsUpdatedEvent extends AbstractEvent {

    public DetectedObjectsUpdatedEvent(long timestamp) {
        super(timestamp);
    }
}
