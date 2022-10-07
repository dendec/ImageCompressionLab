package edu.onu.ddechev.utils;

import edu.onu.ddechev.codecs.Codec;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.*;
import java.util.stream.IntStream;

public class ImageAnalyzer {

    private ImageAnalyzer() {
    }

    public static AnalysisResult analyzeCompression(Image image, Codec codec) {
        AnalysisResult result = new AnalysisResult();
        int size = getSize(image);
        result.addImageProperty("width", getWidth(image));
        result.addImageProperty("height", getHeight(image));
        result.addImageProperty("size, bytes", size);
        result.addImageProperty("colors", getColorsCount(image));
        if (codec != null) {
            long time = System.currentTimeMillis();
            byte[] compressed = codec.compress(image);
            long compressionTime = System.currentTimeMillis() - time;
            time = System.currentTimeMillis();
            Image restoredImage = codec.restore(compressed);
            long restoreTime = System.currentTimeMillis() - time;;
            int compressedSize = compressed.length - Codec.HEADER_SIZE;
            result.addCompressionProperty("compression time, ms", compressionTime);
            result.addCompressionProperty("restore time, ms", restoreTime);
            result.addCompressionProperty("size, bytes", compressedSize);
            result.addCompressionProperty("ratio", Integer.valueOf(size).doubleValue() / compressedSize);
            result.setRestoredImage(restoredImage);
            result.setCompressedData(compressed);
        }
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
}
