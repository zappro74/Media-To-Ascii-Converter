package com.ascii.web.converter;

import java.awt.image.BufferedImage;
import java.util.List;

public class ImageAsciiPipeline implements MediaPipeline
{
    @Override
    public List<String> convert(BufferedImage image, int width, boolean useColor, boolean useDither, AsciiRamp ramp)
    {
        double[][] brightness = ImageLoader.calculatePixels(image);
        double[][] resized = ImageController.resizeOutput(image, brightness, width);

        AsciiRamp chosenRamp = (ramp != null) ? ramp : AsciiRamp.DENSE;

        int numberOfLevels = chosenRamp.getChars().length();
        double[][] dithered = useDither ? ImageController.ditherOutput(resized, numberOfLevels) : resized;

        if (useColor)
        {
            double[][][] rgb = ImageLoader.calculateRgbPixels(image);
            double[][][] resizedRgb = ImageController.resizeRgb(rgb, width);
            return ImageToAscii.toAsciiLinesColored(dithered, resizedRgb, chosenRamp);
        }
        else
        {
            return ImageToAscii.toAsciiLines(dithered, chosenRamp);
        }
    }
}
