package com.ascii.web.converter;

public enum AsciiRamp 
{
    SIMPLE("@%#*+=-:"),
    LIGHT(".:-=+*#%@"),
    DENSE("$@B%8&WM#*oahkbdpqwmZ0OQLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'.");

    private final String chars;

    AsciiRamp(String chars) 
    {
        this.chars = chars;
    }

    public String getChars() 
    {
        return chars;
    }

    public static AsciiRamp fromString(String choice) 
    {
        if (choice == null) return DENSE;
        try
        {
            return AsciiRamp.valueOf(choice.trim().toUpperCase());
        } 
        catch (IllegalArgumentException e) 
        {
            String normalized = choice.trim().toUpperCase();
            switch (normalized) 
            {
                case "SIMPLE":
                    return SIMPLE;
                case "LIGHT":
                    return LIGHT;
                case "DENSE":
                default:
                    return DENSE;
            }
        }
    }
}
