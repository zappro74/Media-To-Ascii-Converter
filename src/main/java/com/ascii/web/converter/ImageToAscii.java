package com.ascii.web.converter;

import java.util.ArrayList;

public class ImageToAscii
{
    public static ArrayList<String> toAsciiLines(double[][] brightness, AsciiRamp rampChoice)
    {
        ArrayList<String> lines = new ArrayList<>(brightness.length);
        String ramp = (rampChoice != null) ? rampChoice.getChars() : AsciiRamp.DENSE.getChars();

        for (int y = 0; y < brightness.length; y++)
        {
            StringBuilder row = new StringBuilder(brightness[0].length);
            for (int x = 0; x < brightness[0].length; x++)
            {
                double value = brightness[y][x];
                int index = (int) Math.round((value / 255.0) * (ramp.length() - 1));
                index = Math.max(0, Math.min(index, ramp.length() - 1));

                row.append(ramp.charAt(index));
            }

            lines.add(row.toString());
        }

        return lines;
    }

    public static ArrayList<String> toAsciiLinesColored(double[][] brightness, double[][][] rgb, AsciiRamp rampChoice)
    {
        ArrayList<String> lines = new ArrayList<>(brightness.length);
        String ramp = (rampChoice != null) ? rampChoice.getChars() : AsciiRamp.DENSE.getChars();

        for (int y = 0; y < brightness.length; y++)
        {
            StringBuilder row = new StringBuilder(brightness[0].length * 20); 
            for (int x = 0; x < brightness[0].length; x++)
            {

                double value = brightness[y][x];
                if (value < 0) 
                {
                    value = 0;
                }
                else if (value > 255) 
                {
                    value = 255;
                }

                int index = (int) Math.round((value / 255.0) * (ramp.length() - 1));
                index = Math.max(0, Math.min(index, ramp.length() - 1));
                char asciiChar = ramp.charAt(index);

                double r = rgb[y][x][0];
                double g = rgb[y][x][1];
                double b = rgb[y][x][2];

                if (r < 0) 
                {
                    r = 0;
                } 
                else if (r > 255) 
                {
                    r = 255;
                }

                if (g < 0) 
                {
                    g = 0;
                }
                else if (g > 255) 
                {
                    g = 255;
                }

                if (b < 0)
                {
                    b = 0;
                } 
                else if (b > 255) 
                {
                    b = 255;
                }

                int red = (int) Math.round(r);
                int green = (int) Math.round(g);
                int blue = (int) Math.round(b);

                row.append("\u001B[38;2;").append(red).append(";").append(green).append(";").append(blue).append("m").append(asciiChar).append("\u001B[0m");
                }

            lines.add(row.toString());
        }
        return lines;
    }
}