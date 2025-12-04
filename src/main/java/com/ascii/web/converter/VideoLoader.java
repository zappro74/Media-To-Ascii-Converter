package com.ascii.web.converter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class VideoLoader
{
    public static ArrayList<File> extractFrames(String ffmpegExecutable, File videoFile, int width) throws IOException, InterruptedException
    {
        ArrayList<File> frames = new ArrayList<>();
        File framesDir = new File("temp_frames_" + System.currentTimeMillis());
        framesDir.mkdirs();

        String pattern = new File(framesDir, "frame_%05d.png").getAbsolutePath();
        String[] args = new String[]
        {
            ffmpegExecutable,
            "-hide_banner",
            "-loglevel", "error",
            "-i", videoFile.getAbsolutePath(),
            "-vf", "scale=" + width + ":-1",
            pattern,
            "-y"
        };
        System.out.println("FFMPEG CMD (frames): " + Arrays.toString(args));

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        StringBuilder stringBuilder = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1)
        {
            stringBuilder.append(new String(buffer, 0, bytesRead));
            if (stringBuilder.length() > 10000) //Safety cap
            {
                break;
            }
        }
        String output = stringBuilder.toString();
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new IOException("ffmpeg failed (exit=" + exitCode + ") output=" + output);
        }

        File[] fileList = framesDir.listFiles();
        if (fileList != null)
        {
            frames = Arrays.stream(fileList).filter(file -> file.getName().toLowerCase().endsWith(".png")).sorted().collect(Collectors.toCollection(ArrayList::new));
        }

        if (frames.isEmpty())
        {
            throw new IOException("No frames extracted; ffmpeg output=" + output);
        }
        
        for (File frame : frames)
        {
            frame.deleteOnExit();
        }
        framesDir.deleteOnExit();

        return frames;
    }
}

