package com.ascii.web.converter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

public class VideoAsciiPipeline implements MediaPipeline
{
    @Override
    public List<String> convert(BufferedImage image, int width, boolean useColor, boolean useDither, AsciiRamp ramp)
    {
        ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
        return pipeline.convert(image, width, useColor, useDither, ramp);
    }

    public File createVideo(String ffmpegExecutable, List<List<String>> asciiFrames, List<double[][][]> colorFrames, File outputFile, int fps, int fontSize, Color backgroundColor, Color textColor, File originalVideoFile) throws IOException, InterruptedException
    {
        if (asciiFrames == null || asciiFrames.isEmpty())
        {
            throw new IllegalArgumentException("No ASCII frames provided");
        }

        File framesDirectory = new File("temp_video_export");
        if (!framesDirectory.exists())
        {
            framesDirectory.mkdirs();
        }

        List<String> firstFrame = asciiFrames.get(0);
        int longestLine = 0;
        for (String line : firstFrame)
        {
            if (line.length() > longestLine)
            {
                longestLine = line.length();
            }
        }

        int characterWidth = (int)(fontSize * 0.6);
        int characterHeight = fontSize;
        int imageWidth = longestLine * characterWidth + 40;
        int imageHeight = firstFrame.size() * characterHeight + 40;

        Font courierFont = new Font("Courier New", Font.PLAIN, fontSize);
        boolean useColor = colorFrames != null && !colorFrames.isEmpty();

        for (int frameIndex = 0; frameIndex < asciiFrames.size(); frameIndex++)
        {
            List<String> currentFrame = asciiFrames.get(frameIndex);
            double[][][] rgbData = useColor ? colorFrames.get(frameIndex) : null;

            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setFont(courierFont);

            graphics.setColor(backgroundColor);
            graphics.fillRect(0, 0, imageWidth, imageHeight);

            int currentY = 20 + characterHeight;
            for (int lineIndex = 0; lineIndex < currentFrame.size(); lineIndex++)
            {
                String line = currentFrame.get(lineIndex);
                int currentX = 20;
                
                for (int charIndex = 0; charIndex < line.length(); charIndex++)
                {
                    char ch = line.charAt(charIndex);
                    
                    if (useColor && rgbData != null && lineIndex < rgbData.length && charIndex < rgbData[lineIndex].length)
                    {
                        double r = Math.max(0, Math.min(255, rgbData[lineIndex][charIndex][0]));
                        double g = Math.max(0, Math.min(255, rgbData[lineIndex][charIndex][1]));
                        double b = Math.max(0, Math.min(255, rgbData[lineIndex][charIndex][2]));
                        graphics.setColor(new Color((int)r, (int)g, (int)b));
                    }
                    else
                    {
                        graphics.setColor(textColor);
                    }
                    
                    graphics.drawString(String.valueOf(ch), currentX, currentY);
                    currentX += characterWidth;
                }
                
                currentY = currentY + characterHeight;
            }

            graphics.dispose();

            String frameName = String.format("frame_%05d.png", frameIndex + 1);
            File frameFile = new File(framesDirectory, frameName);
            ImageIO.write(image, "png", frameFile);
        }

        String framePattern = new File(framesDirectory, "frame_%05d.png").getAbsolutePath();
        String fpsString = String.valueOf(fps);
        String[] ffmpegCommand;
        
        if (originalVideoFile != null && originalVideoFile.exists())
        {
            ffmpegCommand = new String[]
            {
                ffmpegExecutable,
                "-framerate", fpsString,
                "-i", framePattern,
                "-i", originalVideoFile.getAbsolutePath(),
                "-map", "0:v:0",
                "-map", "1:a:0?",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "22",
                "-threads", "4",
                "-c:a", "copy",
                "-shortest",
                "-pix_fmt", "yuv420p",
                "-y",
                outputFile.getAbsolutePath()
            };
        }
        else
        {
            ffmpegCommand = new String[]
            {
                ffmpegExecutable,
                "-framerate", fpsString,
                "-i", framePattern,
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "22",
                "-threads", "4",
                "-pix_fmt", "yuv420p",
                "-y",
                outputFile.getAbsolutePath()
            };
        }

        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        InputStream processOutput = process.getInputStream();
        StringBuilder outputText = new StringBuilder();
        byte[] readBuffer = new byte[1024];
        int bytesRead = processOutput.read(readBuffer);
        while (bytesRead != -1)
        {
            outputText.append(new String(readBuffer, 0, bytesRead));
            bytesRead = processOutput.read(readBuffer);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new IOException("ffmpeg failed with exit code " + exitCode + ": " + outputText.toString());
        }

        File[] frameFiles = framesDirectory.listFiles();
        if (frameFiles != null)
        {
            for (File file : frameFiles)
            {
                file.delete();
            }
        }
        framesDirectory.delete();

        return outputFile;
    }
}
