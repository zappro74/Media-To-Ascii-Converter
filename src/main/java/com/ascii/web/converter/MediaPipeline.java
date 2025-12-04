package com.ascii.web.converter;

import java.awt.image.BufferedImage;
import java.util.List;

public interface MediaPipeline
{
    List<String> convert(BufferedImage image, int width, boolean useColor, boolean useDither, AsciiRamp ramp);
}
