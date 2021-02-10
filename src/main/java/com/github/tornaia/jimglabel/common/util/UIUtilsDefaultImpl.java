package com.github.tornaia.jimglabel.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
public class UIUtilsDefaultImpl implements UIUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UIUtilsDefaultImpl.class);

    @Autowired
    public UIUtilsDefaultImpl() {
        Dimension screenSize = getScreenSize();
        LOG.info("Screen size, width: {}, height: {}", screenSize.width, screenSize.height);
    }

    @Override
    public boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    @Override
    public void invokeLater(String label, Runnable runnable) {
        LOG.trace("Runnable pushed, label: {}", label);
        long start = System.nanoTime();

        SwingUtilities.invokeLater(() -> {
            long blockedDuration = (System.nanoTime() - start) / 1_000_000;
            if (blockedDuration > 250) {
                LOG.warn("Runnable blocked, label: {}, blocked duration: {} ms", label, blockedDuration);
            } else {
                LOG.trace("Runnable blocked, label: {}, blocked duration: {} ms", label, blockedDuration);
            }

            long executionStart = System.nanoTime();

            runnable.run();

            long executionDuration = (System.nanoTime() - executionStart) / 1_000_000;
            if (executionDuration > 200) {
                LOG.warn("Runnable completed, label: {}, execution duration: {} ms", label, executionDuration);
            } else {
                LOG.trace("Runnable completed, label: {}, execution duration: {} ms", label, executionDuration);
            }
        });
    }

    @Override
    public Dimension getScreenSize() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        return toolkit.getScreenSize();
    }
}
