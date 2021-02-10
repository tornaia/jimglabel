package com.github.tornaia.jimglabel.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventPublisherDefaultImpl implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(EventPublisherDefaultImpl.class);

    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public EventPublisherDefaultImpl(org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(AbstractEvent abstractEvent) {
        LOG.debug("Publish event: {}", abstractEvent);

        try {
            applicationEventPublisher.publishEvent(abstractEvent);
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            boolean ignoreException = message != null && message.contains("has been closed already");
            if (ignoreException) {
                return;
            }
            throw e;
        } catch (BeanCreationNotAllowedException e) {
            String message = e.getMessage();
            boolean ignoreException = message != null && message.contains("Singleton bean creation not allowed while singletons of this factory are in destruction");
            if (ignoreException) {
                return;
            }
            throw e;
        }
    }
}
