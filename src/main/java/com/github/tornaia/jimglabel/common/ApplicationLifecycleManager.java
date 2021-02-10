package com.github.tornaia.jimglabel.common;

import com.github.tornaia.jimglabel.common.event.ExitApplicationEvent;
import com.github.tornaia.jimglabel.common.util.UncaughtThrowableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationLifecycleManager.class);

    private final UncaughtThrowableService uncaughtThrowableService;

    @Autowired
    public ApplicationLifecycleManager(UncaughtThrowableService uncaughtThrowableService) {
        this.uncaughtThrowableService = uncaughtThrowableService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void applicationStarted() {
        LOG.info("Application starting");
        try {
            uncaughtThrowableService.create();
            LOG.info("Application started");
        } catch (RuntimeException e) {
            LOG.error("Failed to start application", e);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void applicationStopped() {
        LOG.info("Application stopped");
    }

    @Async
    @EventListener(ExitApplicationEvent.class)
    public void exitApplication() {
        uncaughtThrowableService.remove();
        System.exit(0);
    }
}
