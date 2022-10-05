package edu.onu.ddechev.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.stream.IntStream;

public class ImageAnalyzer {
    private ImageAnalyzer() {
    }

    public static List<Map<String, String>> analyzeImage(Image image) {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(getProperty("width", String.valueOf(getWidth(image))));
        result.add(getProperty("height", String.valueOf(getHeight(image))));
        result.add(getProperty("size", String.valueOf(getSize(image))));
        result.add(getProperty("colors", String.valueOf(getColorsCount(image))));
        return result;
    }

    private static int getWidth(Image image) {
        return Double.valueOf(image.getWidth()).intValue();
    }

    private static int getHeight(Image image) {
        return Double.valueOf(image.getHeight()).intValue();
    }

    private static int getSize(Image image) {
        return getWidth(image) * getHeight(image) * 3;
    }

    private static int getColorsCount(Image image) {
        PixelReader pixelReader = image.getPixelReader();
        Set<Integer> colors = new HashSet<>();
        IntStream.range(0, getHeight(image)).forEach(y ->
            IntStream.range(0, getWidth(image)).forEach(x -> {
                colors.add(pixelReader.getArgb(x, y));
            })
        );
        return colors.size();
    }

    private static Map<String, String> getProperty(String property, String value) {
        return Map.of("property", property, "value", value);
    }
}
