package com.github.tornaia.jimglabel.common.util;

import java.awt.*;

public interface UIUtils {

    boolean isHeadless();

    void invokeLater(String label, Runnable runnable);

    Dimension getScreenSize();
}
