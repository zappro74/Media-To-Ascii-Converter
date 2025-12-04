package com.ascii.web.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoToAscii
{
    public static ArrayList<ArrayList<String>> toAsciiFrames(String ffmpegExecutable, File videoFile, int width, boolean useColor, boolean useDither, AsciiRamp ramp) throws Exception
    {
        ArrayList<ArrayList<String>> framesAscii = new ArrayList<>();

        ArrayList<File> frames = VideoLoader.extractFrames(ffmpegExecutable, videoFile, width);
        ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
        for (int index = 0; index < frames.size(); index++)
        {
            File frame = frames.get(index);
            BufferedImage image = ImageLoader.loadImage(frame);
            List<String> lines = pipeline.convert(image, width, useColor, useDither, ramp);
            framesAscii.add(new ArrayList<>(lines));
        }

        return framesAscii;
    }
}
