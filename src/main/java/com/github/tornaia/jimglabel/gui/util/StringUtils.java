package com.github.tornaia.jimglabel.gui.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class StringUtils {

    private StringUtils() {
    }

    /**
     * If we pass à, the method returns a + ` .
     * Then using a regular expression, we clean up the string to keep only valid US-ASCII characters.
     * @param src input
     * @return normalized string
     */
    public static String normalize(String src) {
        String temp = Normalizer.normalize(src, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("");
    }
}
