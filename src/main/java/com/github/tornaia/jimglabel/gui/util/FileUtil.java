package com.github.tornaia.jimglabel.gui.util;

import java.text.DecimalFormat;

public final class FileUtil {

    private FileUtil() {
    }

    public static String readableFileSize(long size) {
        if (size < 0) {
            return "error";
        }
        if (size == 0) {
            return "0B";
        }
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
