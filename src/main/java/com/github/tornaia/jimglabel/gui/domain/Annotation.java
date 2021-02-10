package com.github.tornaia.jimglabel.gui.domain;

import java.util.List;

public record Annotation(String name, long size, int width, int height, List<DetectedObject> objects) {
}
