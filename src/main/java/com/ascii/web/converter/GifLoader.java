package com.ascii.web.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class GifLoader
{
    public static List<BufferedImage> loadFrames(File file)
    {
        List<BufferedImage> frames = new ArrayList<>();
        try (ImageInputStream stream = ImageIO.createImageInputStream(file))
        {
            if (stream == null)
            {
                throw new IllegalArgumentException("Cannot read GIF: " + file.getPath());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext())
            {
                throw new IllegalStateException("No GIF reader available");
            }
            ImageReader reader = readers.next();
            reader.setInput(stream, false, false);
            int num = reader.getNumImages(true);
            for (int i = 0; i < num; i++)
            {
                BufferedImage frame = reader.read(i);
                if (frame != null)
                {
                    frames.add(frame);
                }
            }
            reader.dispose();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read GIF frames from " + file.getPath(), e);
        }
        return frames;
    }
}
