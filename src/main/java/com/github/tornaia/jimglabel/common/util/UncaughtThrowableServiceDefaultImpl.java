package com.github.tornaia.jimglabel.common.util;

import com.github.tornaia.jimglabel.common.event.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UncaughtThrowableServiceDefaultImpl implements UncaughtThrowableService {

    private static final Logger LOG = LoggerFactory.getLogger(UncaughtThrowableServiceDefaultImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public UncaughtThrowableServiceDefaultImpl(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void create() {
        Thread.setDefaultUncaughtExceptionHandler(handleUncaughtExceptions());
    }

    @Override
    public void remove() {
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    private Thread.UncaughtExceptionHandler handleUncaughtExceptions() {
        return (thread, throwable) -> {
            LOG.error("Uncaught throwable", throwable);
            applicationEventPublisher.exit();
        };
    }
}
