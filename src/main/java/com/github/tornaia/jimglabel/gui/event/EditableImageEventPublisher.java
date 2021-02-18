package com.github.tornaia.jimglabel.gui.event;

import com.github.tornaia.jimglabel.common.clock.ClockService;
import com.github.tornaia.jimglabel.common.event.EventPublisher;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EditableImageEventPublisher {

    private final EventPublisher eventPublisher;
    private final ClockService clockService;

    @Autowired
    public EditableImageEventPublisher(EventPublisher eventPublisher, ClockService clockService) {
        this.eventPublisher = eventPublisher;
        this.clockService = clockService;
    }

    public void updateSelectedImage(EditableImage image) {
        eventPublisher.publish(new EditableImageUpdatedEvent(image, clockService.now()));
    }

    public void selectDetectedObject(DetectedObject detectedObject) {
        eventPublisher.publish(new DetectedObjectSelectedEvent(detectedObject, clockService.now()));
    }

    public void updateDetectedObjects() {
        eventPublisher.publish(new DetectedObjectsUpdatedEvent(clockService.now()));
    }
}
