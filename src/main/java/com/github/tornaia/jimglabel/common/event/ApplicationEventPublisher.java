package com.github.tornaia.jimglabel.common.event;

import com.github.tornaia.jimglabel.common.clock.ClockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEventPublisher {

    private final EventPublisher eventPublisher;
    private final ClockService clockService;

    @Autowired
    public ApplicationEventPublisher(EventPublisher eventPublisher, ClockService clockService) {
        this.eventPublisher = eventPublisher;
        this.clockService = clockService;
    }

    public void exit() {
        eventPublisher.publish(new ExitApplicationEvent(clockService.now()));
    }
}
