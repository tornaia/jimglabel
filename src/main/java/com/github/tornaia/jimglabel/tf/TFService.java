package com.github.tornaia.jimglabel.tf;

import java.awt.image.BufferedImage;
import java.util.List;

public interface TFService {

    List<Detection> detect(BufferedImage inputImage);
}
