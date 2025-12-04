package com.ascii.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ascii.web.converter.AsciiRamp;
import com.ascii.web.converter.GifAsciiPipeline;
import com.ascii.web.converter.GifLoader;
import com.ascii.web.converter.ImageAsciiPipeline;
import com.ascii.web.converter.ImageController;
import com.ascii.web.converter.ImageLoader;
import com.ascii.web.converter.ImageToAscii;
import com.ascii.web.converter.VideoAsciiPipeline;
import com.ascii.web.converter.VideoLoader;

@Service
public class AsciiService
{
    @Value("${ffmpeg.executable:ffmpeg}")
    private String ffmpegExecutable;

    public File handleVideoExport(MultipartFile file, int width, boolean useColor, boolean useDither, String rampParam, int fps, int fontSize) throws Exception
    {
        File tempFile = File.createTempFile("upload", getExtension(file.getOriginalFilename()));
        file.transferTo(tempFile);

        AsciiRamp ramp = AsciiRamp.fromString(rampParam);
        int characterPixelWidth = (int)(fontSize * 0.6);
        int targetPixelWidth = Math.max(200, width * characterPixelWidth + 40);
        ArrayList<File> frames = VideoLoader.extractFrames(ffmpegExecutable, tempFile, targetPixelWidth);
        ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
        List<List<String>> asciiFrames = new ArrayList<>();
        List<double[][][]> colorFrames = useColor ? new ArrayList<>() : null;
        
        for (File frame : frames)
        {
            BufferedImage image = ImageLoader.loadImage(frame);
            
            double[][] brightness = ImageLoader.calculatePixels(image);
            double[][] resized = ImageController.resizeOutput(image, brightness, width);
            int numberOfLevels = ramp.getChars().length();
            double[][] dithered = useDither ? ImageController.ditherOutput(resized, numberOfLevels) : resized;
            
            List<String> lines = ImageToAscii.toAsciiLines(dithered, ramp);
            asciiFrames.add(lines);
            
            if (useColor)
            {
                double[][][] rgb = ImageLoader.calculateRgbPixels(image);
                double[][][] resizedRgb = ImageController.resizeRgb(rgb, width);
                colorFrames.add(resizedRgb);
            }
        }

        File outputVideo = File.createTempFile("ascii_video", ".mp4");
        VideoAsciiPipeline videoPipeline = new VideoAsciiPipeline();
        videoPipeline.createVideo(ffmpegExecutable, asciiFrames, colorFrames, outputVideo, fps, fontSize, Color.BLACK, Color.WHITE, tempFile);
        
        tempFile.delete();
        
        return outputVideo;
    }

    public ArrayList<ArrayList<String>> handleVideoConversion(MultipartFile file, int width, boolean useColor, boolean useDither, String rampParam) throws Exception
    {
        File tempFile = File.createTempFile("upload", getExtension(file.getOriginalFilename()));
        file.transferTo(tempFile);

        try
        {
            AsciiRamp ramp = AsciiRamp.fromString(rampParam);
            ArrayList<File> frames = VideoLoader.extractFrames(ffmpegExecutable, tempFile, width);
            ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
            ArrayList<ArrayList<String>> result = new ArrayList<>();
            for (File frame : frames)
            {
                BufferedImage image = ImageLoader.loadImage(frame);
                List<String> lines = pipeline.convert(image, width, useColor, useDither, ramp);
                result.add(new ArrayList<>(lines));
            }
            return result;
        }
        finally
        {
            tempFile.delete();
        }
    }

    public ArrayList<ArrayList<String>> handleGifConversion(MultipartFile file, int width, boolean useColor, boolean useDither, String rampParam) throws Exception
    {
        File tempFile = File.createTempFile("upload", getExtension(file.getOriginalFilename()));
        file.transferTo(tempFile);

        try
        {
            AsciiRamp ramp = AsciiRamp.fromString(rampParam);
            List<BufferedImage> frames = GifLoader.loadFrames(tempFile);
            GifAsciiPipeline pipeline = new GifAsciiPipeline();
            ArrayList<ArrayList<String>> result = new ArrayList<>();
            for (BufferedImage frame : frames)
            {
                List<String> lines = pipeline.convert(frame, width, useColor, useDither, ramp);
                result.add(new ArrayList<>(lines));
            }
            return result;
        }
        finally
        {
            tempFile.delete();
        }
    }

    public File handleGifExport(MultipartFile file, int width, boolean useColor, boolean useDither, String rampParam, int fps, int fontSize) throws Exception
    {
        File tempFile = File.createTempFile("upload", getExtension(file.getOriginalFilename()));
        file.transferTo(tempFile);

        Path framesDir = null;
        try
        {
            AsciiRamp ramp = AsciiRamp.fromString(rampParam);
            List<BufferedImage> gifFrames = GifLoader.loadFrames(tempFile);

            framesDir = Files.createTempDirectory("ascii_gif_frames_");

            BufferedImage firstGifFrame = gifFrames.get(0);
            double[][] firstBrightness = ImageLoader.calculatePixels(firstGifFrame);
            double[][] firstResized = ImageController.resizeOutput(firstGifFrame, firstBrightness, width);
            int numberOfLevels = ramp.getChars().length();
            double[][] firstDithered = useDither ? ImageController.ditherOutput(firstResized, numberOfLevels) : firstResized;
            List<String> firstAscii = ImageToAscii.toAsciiLines(firstDithered, ramp);
            
            int longestLine = firstAscii.stream().mapToInt(line -> line.length()).max().orElse(0);

            int characterWidth = (int)(fontSize * 0.6);
            int characterHeight = fontSize;
            int imageWidth = longestLine * characterWidth + 40;
            int imageHeight = firstAscii.size() * characterHeight + 40;

            Font courierFont = new Font("Courier New", Font.PLAIN, fontSize);

            for (int i = 0; i < gifFrames.size(); i++)
            {
                BufferedImage src = gifFrames.get(i);
                
                double[][] brightness = ImageLoader.calculatePixels(src);
                double[][] resized = ImageController.resizeOutput(src, brightness, width);
                double[][] dithered = useDither ? ImageController.ditherOutput(resized, numberOfLevels) : resized;
                List<String> ascii = ImageToAscii.toAsciiLines(dithered, ramp);
                
                double[][][] rgbData = null;
                if (useColor)
                {
                    double[][][] rgb = ImageLoader.calculateRgbPixels(src);
                    rgbData = ImageController.resizeRgb(rgb, width);
                }

                BufferedImage out = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.setFont(courierFont);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, imageWidth, imageHeight);
                
                int y = 20 + characterHeight;
                for (int lineIndex = 0; lineIndex < ascii.size(); lineIndex++)
                {
                    String line = ascii.get(lineIndex);
                    int x = 20;
                    
                    for (int charIndex = 0; charIndex < line.length(); charIndex++)
                    {
                        char ch = line.charAt(charIndex);
                        
                        if (useColor && rgbData != null && lineIndex < rgbData.length && charIndex < rgbData[lineIndex].length)
                        {
                            double r = Math.max(0, Math.min(255, rgbData[lineIndex][charIndex][0]));
                            double gVal = Math.max(0, Math.min(255, rgbData[lineIndex][charIndex][1]));
                            double b = Math.max(0, Math.min(255, rgbData[lineIndex][charIndex][2]));
                            g.setColor(new Color((int)r, (int)gVal, (int)b));
                        }
                        else
                        {
                            g.setColor(Color.WHITE);
                        }
                        
                        g.drawString(String.valueOf(ch), x, y);
                        x += characterWidth;
                    }
                    y += characterHeight;
                }
                g.dispose();

                Path framePath = framesDir.resolve(String.format("frame_%05d.png", i + 1));
                ImageIO.write(out, "png", framePath.toFile());
            }

            File outputGif = File.createTempFile("ascii_gif", ".gif");
            String[] cmd = new String[]
            {
                ffmpegExecutable,
                "-framerate", String.valueOf(fps),
                "-i", framesDir.resolve("frame_%05d.png").toFile().getAbsolutePath(),
                "-loop", "0",
                "-y",
                outputGif.getAbsolutePath()
            };

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (InputStream is = p.getInputStream())
            {
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) 
                { 
                    //discard ffmpeg output 
                }
            }
            int code = p.waitFor();
            if (code != 0)
            {
                throw new RuntimeException("ffmpeg failed to create GIF, exit code: " + code);
            }

            //Cleanup temp frames
            File[] files = framesDir.toFile().listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    f.delete();
                }
            }
            framesDir.toFile().delete();

            return outputGif;
        }
        finally
        {
            tempFile.delete();
            if (framesDir != null)
            {
                File[] files = framesDir.toFile().listFiles();
                if (files != null)
                {
                    for (File f : files) { f.delete(); }
                }
                framesDir.toFile().delete();
            }
        }
    }

    public String handleConversion(MultipartFile file, int width, boolean useColor, boolean useDither, String rampParam) throws Exception
    {
        File tempFile = File.createTempFile("upload", getExtension(file.getOriginalFilename()));
        file.transferTo(tempFile);

        try
        {
            BufferedImage image = ImageLoader.loadImage(tempFile);
            AsciiRamp ramp = AsciiRamp.fromString(rampParam);
            ImageAsciiPipeline pipeline = new ImageAsciiPipeline();
            List<String> lines = pipeline.convert(image, width, useColor, useDither, ramp);
            String ascii = String.join("\n", lines);
            return ascii;
        }
        finally
        {
            tempFile.delete();
        }
    }

    private String getExtension(String fileName)
    {
        if (fileName == null) return ".tmp";
        int index = fileName.lastIndexOf('.');
        if (index == -1) return ".tmp";
        return fileName.substring(index);
    }
}
