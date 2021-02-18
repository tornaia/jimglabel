package com.github.tornaia.jimglabel.gui.event;

import com.github.tornaia.jimglabel.common.event.AbstractEvent;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;

public class DetectedObjectSelectedEvent extends AbstractEvent {

    private final DetectedObject detectedObject;

    public DetectedObjectSelectedEvent(DetectedObject detectedObject, long timestamp) {
        super(timestamp);
        this.detectedObject = detectedObject;
    }

    public DetectedObject getDetectedObject() {
        return detectedObject;
    }
}
