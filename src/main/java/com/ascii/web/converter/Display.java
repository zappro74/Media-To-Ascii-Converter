package com.ascii.web.converter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Display
{
    public static void printToConsole(ArrayList<String> lines)
    {
        lines.forEach(line -> System.out.println(line));
    }

    public static void saveToTxtFile(ArrayList<String> lines, String filePath)
    {
        try (FileWriter writer = new FileWriter(filePath))
        {
            for (String line : lines)
            {
                writer.write(line + System.lineSeparator());
            }
            System.out.println("Output successfully written to: " + filePath);
        }
        catch (IOException e)
        {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }
}