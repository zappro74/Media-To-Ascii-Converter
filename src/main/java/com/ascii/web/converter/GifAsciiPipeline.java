package com.ascii.web.converter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GifAsciiPipeline implements MediaPipeline
{
    @Override
    public List<String> convert(BufferedImage image, int width, boolean useColor, boolean useDither, AsciiRamp ramp)
    {
        ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
        return pipeline.convert(image, width, useColor, useDither, ramp);
    }

    public List<List<String>> convertFrames(List<BufferedImage> frames, int width, boolean useColor, boolean useDither, AsciiRamp ramp)
    {
        List<List<String>> asciiFrames = new ArrayList<>();
        ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
        for (BufferedImage frame : frames)
        {
            List<String> lines = pipeline.convert(frame, width, useColor, useDither, ramp);
            asciiFrames.add(lines);
        }
        return asciiFrames;
    }
}
